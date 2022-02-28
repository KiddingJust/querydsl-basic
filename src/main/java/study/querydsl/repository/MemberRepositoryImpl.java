package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static com.querydsl.core.types.ExpressionUtils.count;
import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMemberDSL.memberDSL;
import static study.querydsl.entity.QTeamDSL.teamDSL;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        memberDSL.id.as("memberId"),
                        memberDSL.username,
                        memberDSL.age,
                        teamDSL.id.as("teamId"),
                        teamDSL.name.as("teamName")
                ))
                .from(memberDSL)
                .leftJoin(memberDSL.team, teamDSL)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }
    //Predicate가 아닌, BooleanExpression으로 해야 조합 가능.
    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? memberDSL.username.eq(username) : null ;
    }
    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? teamDSL.name.eq(teamName) : null;
    }
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? memberDSL.age.goe(ageGoe) : null;
    }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? memberDSL.age.loe(ageLoe) : null;
    }

    /** 이건 잘못된 메서드. */
    @Override
    public List<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        return queryFactory
                .select(new QMemberTeamDto(
                        memberDSL.id.as("memberId"),
                        memberDSL.username,
                        memberDSL.age,
                        teamDSL.id.as("teamId"),
                        teamDSL.name.as("teamName")
                ))
                .from(memberDSL)
                .leftJoin(memberDSL.team, teamDSL)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /** 데이터와 전체 카운트 별도로 조회. 위의 Simple 버전은 원래 fetchResult 쓰려 했던 것 */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                                .select(new QMemberTeamDto(
                                        memberDSL.id.as("memberId"),
                                        memberDSL.username,
                                        memberDSL.age,
                                        teamDSL.id.as("teamId"),
                                        teamDSL.name.as("teamName")
                                ))
                                .from(memberDSL)
                                .leftJoin(memberDSL.team, teamDSL)
                                .where(usernameEq(condition.getUsername()),
                                        teamNameEq(condition.getTeamName()),
                                        ageGoe(condition.getAgeGoe()),
                                        ageLoe(condition.getAgeLoe()))
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

        /** fetchCount(), fetchResult() deprecated에 따른 count 쿼리 */
        JPAQuery<Long> countQuery = queryFactory
                .select(memberDSL.count())
                .from(memberDSL)
                .leftJoin(memberDSL.team, teamDSL)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        /**
         이렇게 리턴하면, 맨 마지막 fetchOne 메서드는, content와 pageable이 조건에 맞을 때만 실행됨.
         첫번째 페이지 사이즈가 100인데 컨텐츠 갯수가 3이면, 토탈 카운트 날릴 필요가 없음.
         마지막은 람다식으로 풀면 () -> countQuery.fetchOne()
         */
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        //return new PageImpl<>(content, pageable, total);
    }
}
