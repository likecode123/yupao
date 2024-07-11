package com.yupi.usercenter.contant.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercenter.config.common.BaseResponse;
import com.yupi.usercenter.config.common.ErrorCode;
import com.yupi.usercenter.config.common.ResultUtils;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Team;

import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.dto.TeamQuery;
import com.yupi.usercenter.model.request.DeleteRequest;
import com.yupi.usercenter.model.request.TeamJoinRequest;
import com.yupi.usercenter.model.request.TeamQuitRequest;
import com.yupi.usercenter.model.request.TeamUpdateRequest;
import com.yupi.usercenter.model.vo.TeamUserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("team")
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
@Slf4j
public class TeamController {
    @Resource
    private TeamService teamService;
    @Resource
    private UserService userService;
    @Resource
    private UserTeamService userTeamService;

    @PostMapping("add")
    public BaseResponse<Long> addTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long result = teamService.addTeam(team,loginUser);
        if (result== 0 ) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入失败");
        }
        return ResultUtils.success(team.getId());
    }



//    @PostMapping("update")
//    public BaseResponse<Boolean> deleteTeam(@RequestBody Team team) {
//        if (team == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        //updateById对应数据表的主键
//        boolean result = teamService.updateById(team);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
//        }
//        return ResultUtils.success(true);
//    }
    @PostMapping("update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser==null){
            throw new  BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        //updateById对应数据表的主键
        boolean result = teamService.updateTeam(teamUpdateRequest,loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
        }
        return ResultUtils.success(true);
    }
    //查询数据
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam Long teamId) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return ResultUtils.success(team);
    }


    @GetMapping("list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery,HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);

        //传入querywrapper,是条件构造器 构造的时候只需要指定 表名 和查询条件
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,isAdmin);
        List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());

        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();

        try{
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId",loginUser.getId());
            userTeamQueryWrapper.in("teamId",teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team ->{
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        }catch (Exception e){
        }
/**
 * 查询已加入队伍的人数
 */
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
//根据teamId进行分组
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().
                collect(Collectors.groupingBy(UserTeam::getTeamId));
//给hasJoinNum设置值
        teamList.forEach(team ->
                team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new
                        ArrayList<>()).size()));

        return ResultUtils.success(teamList);
    }

    /**
     * 获取我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        boolean isAdmin = userService.isAdmin(request);
        //传入querywrapper,是条件构造器 构造的时候只需要指定 表名 和查询条件
        List<TeamUserVO> userList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(userList);
    }

    /**
     * 获取我加入的队伍
     * @param teamQuery
     * @return
     */
    @GetMapping("list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper();
        queryWrapper
                .eq("userId",loginUser.getId());


        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //取出不重复的队伍id
//      //.collect(Collectors.groupingBy(UserTeam::getTeamId)) -
//      这是一个终端操作，用于收集流中的元素。Collectors.groupingBy
//      是一个收集器（Collector），它根据提供的函数（在这个例子中
//      是 UserTeam::getTeamId，一个方法引用）对流中的元素进行分组
//      。getTeamId 应该是 UserTeam 类的一个方法，返回某个用于分组
//      的关键字段（比如团队ID）。
        //1,2    1,3    2,3
        //=>     2  3
        //=>     1   2,3
        Map<Long,List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        //取键的集合
        List<Long> isList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(isList);

        //传入querywrapper,是条件构造器 构造的时候只需要指定 表名 和查询条件
        List<TeamUserVO> userList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(userList);
    }
//todo 调用分页
    @GetMapping("list/page")
    public BaseResponse<Page<Team>> listPagesTeams(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery,team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
       //针对Team表的查询
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        //传入querywrapper,是条件构造器 构造的时候只需要指定 表名 和查询条件
        Page<Team> pageResult = teamService.page(page,queryWrapper);
        return ResultUtils.success(pageResult);
    }

    @PostMapping("join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        if (deleteRequest==null || deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.deleteTeam(id, loginUser);
        return ResultUtils.success(result);
    }

//    @GetMapping("getTeamById")
//    public Team getTeamById(long teamId) {
//        if (teamId==0){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"teamId不能为空");
//        }
//        QueryWrapper<Team> queryWrapper =new QueryWrapper<>();
//        queryWrapper.eq("teamId",teamId);
//        Team team = teamService.getOne(queryWrapper);
//        return team;
//
//    }
}