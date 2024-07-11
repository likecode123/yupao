package com.yupi.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.config.common.BaseResponse;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.dto.TeamQuery;
import com.yupi.usercenter.model.request.DeleteRequest;
import com.yupi.usercenter.model.request.TeamJoinRequest;
import com.yupi.usercenter.model.request.TeamQuitRequest;
import com.yupi.usercenter.model.request.TeamUpdateRequest;
import com.yupi.usercenter.model.vo.TeamUserVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 *
 */
public interface TeamService extends IService<Team> {

    @Transactional
    Long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    Boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);



    @Transactional(rollbackFor = Exception.class)
    Boolean deleteTeam(long id, User loginUser);
}
