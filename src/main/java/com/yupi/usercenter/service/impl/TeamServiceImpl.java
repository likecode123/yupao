package com.yupi.usercenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.config.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.mapper.TeamMapper;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.dto.TeamQuery;
import com.yupi.usercenter.model.enums.TeamStatusEnum;
import com.yupi.usercenter.model.request.TeamJoinRequest;
import com.yupi.usercenter.model.request.TeamQuitRequest;
import com.yupi.usercenter.model.request.TeamUpdateRequest;
import com.yupi.usercenter.model.vo.TeamUserVO;
import com.yupi.usercenter.model.vo.UserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;



    @Override
    @Transactional
    public Long addTeam(Team team, User loginUser) {
        //1.请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        final long userId = loginUser.getId();
//3.检验信息
//(1).队伍人数>1且<=20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);//如果为空，直接赋值

        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
//(2).队伍名称 <=20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
// (3) 描述<= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
//(4)status 是否公开，不传默认为0
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
//(5)如果status是加密状态，一定要密码 且密码<=32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
//(6)超出时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超出时间 > 当前时间");
        }
//(7)校验用户最多创建5个队伍
//todo 有bug。可能同时创建100个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
        }
//4.插入队伍消息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        // 这里我们因为完善了业务层，所以在controller层可以简便下代码，我们需要新建一个队伍添加请求封装类（便于
        // 前端知道该输入哪些参数） 新的请求封装类位于model包里的request包。
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
//5. 插入用户 ==> 队伍关系 到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {


        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            //增加List
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)){
                queryWrapper.in("id",idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }

        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (
                Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员并且当前用户 可以修改
        if (oldTeam.getUserId() != loginUser.getId() && userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //如果队伍状态改为加密  必须有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密状态下必须有密码");
            }

        }


        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        boolean result = this.updateById(updateTeam);
        return result;
    }

    @Override
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {

        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        //既然有teamId，也就是存在这个队伍，通过id找到这个队伍 然后判断是否满人  是否过期
        Team team = this.getById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        //如果加入的队伍是加密的，必须密码匹配才可以
        Integer status = team.getStatus();
        TeamStatusEnum statusEnum =TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == TeamStatusEnum.PRIVATE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }

        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET == statusEnum){
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        //把需要数据库查询的放在后面 提高性能。查询该用户加入的队伍数量
        long userId = loginUser.getId();

        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());

                    //count里面是一个wrapper  因此 新建一个querywrapper
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", userId);
                    long count = userTeamService.count(queryWrapper);
                    if (count > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍过多");
                    }

                    //不能够重复加入已经加入的队伍
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId", teamId);
                    queryWrapper.eq("userId", userId);
                    //同时满足这两个查询条件 team_id=teamId userId=userId
                    long unionCount = userTeamService.count(queryWrapper);
                    //这里应该是大于0   临界值
                    if (unionCount > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复加入");
                    }


                    //只能加入未满的队伍,刚才的count是这个id加入了多少队伍  现在查询这个队伍一共多少用户
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId", teamId);
                    long userCount = userTeamService.count(queryWrapper);
                    if (userCount >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    //修改队伍信息表示用户加入队伍 开始操作
                    UserTeam userTeam = new UserTeam();

                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        }catch (InterruptedException e ){
            log.error("doCacheRecommendUser error ",e);
            return false;
        }finally {
            if (lock.isHeldByCurrentThread()){
                System.out.println("unlock:"+Thread.currentThread().getId());
                lock.unlock();
            }
        }
        }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不存在");
        }
        //判断队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        //判断我是否在队伍中
        Long userId = loginUser.getId();
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        //果 userTeam 中的属性被赋予了值，这些值将被用作查询的初始条件。这是一种快速设置查询条件的方式，尤其当你想基于某个已存在的实体的状态来构建查询时非常有效。
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "你不是队伍成员");
        }

        //查询某个队伍已经加入的人数
        QueryWrapper userCountQueryWrapper = new QueryWrapper<>();
        userCountQueryWrapper.eq("teamId", teamId);
        long userCount = userTeamService.count(userCountQueryWrapper);
        if (userCount == 1) {
            //删除队伍和 用户加入队伍  的关系
            this.removeById(teamId);

        } else {
            //要删除的这个队伍不止一个人
            //是否为队长
            if (team.getUserId().equals(userId)) {
                //把队伍转移给最早加入的用户
                //1 查询已经加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by createTime asc limit 2");

                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextUserId = nextUserTeam.getUserId();
                //更新队长
                Team team1 = new Team();
                team1.setUserId(nextUserId);
                team1.setId(teamId);
                boolean result = this.updateById(team1);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队长更新失败");
                }
            }
        }


        return userTeamService.remove(queryWrapper);


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTeam(long id, User loginUser) {
        // 1. 校验 team 是否存在
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        long teamId=team.getId();
        // 2. 校验是否是队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"非队长无权限删除队伍");
        }
        // 3. 删除 team 对应的所有 team_user
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除 team 失败");
        }
        // 4. 删除 team
        QueryWrapper<Team> postQueryWrapper = new QueryWrapper<>();
        postQueryWrapper.eq("id", teamId);
        boolean postResult = this.remove(postQueryWrapper);
        if (!postResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除 team 失败");
        }
        return true;

    }
}











