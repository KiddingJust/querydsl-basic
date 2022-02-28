package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.MemberDSL;
import study.querydsl.entity.QMemberDSL;
import study.querydsl.entity.QTeamDSL;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMemberDSL.*;
import static study.querydsl.entity.QTeamDSL.*;


@Repository
//@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    //이렇게 주입 시에 queryFactory를 만들어줘도 되고,
    //메인클래스 등에 @Bean으로 등록해줘도 되긴 한다.
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(MemberDSL member) {
        em.persist(member);
    }

    public Optional<MemberDSL> findById(Long id) {
        MemberDSL findMember = em.find(MemberDSL.class, id);
        return Optional.of(findMember);
    }

    public List<MemberDSL> findAll() {
        return em.createQuery("select m from MemberDSL m", MemberDSL.class)
                .getResultList();
    }

    public List<MemberDSL> findAll_Querydsl() {
        return queryFactory
                .selectFrom(memberDSL)
                .fetch();
    }

    public List<MemberDSL> findByUsername(String username) {
        return em.createQuery("select m from MemberDSL m" +
                        " where m.username = :username", MemberDSL.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<MemberDSL> findByUsername_QueryDSL(String username) {
        return queryFactory
                .selectFrom(memberDSL)
                .where(memberDSL.username.eq(username))
                .fetch();
    }

    //검색 조건 추가 (동적 쿼리)
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
        //builder
        BooleanBuilder builder = new BooleanBuilder();
        //username이 null이나 "" 등으로 넘어올 수 있음. 이때 hasText 쓰면 됨.
        if(hasText(condition.getUsername())){
            builder.and(memberDSL.username.eq(condition.getUsername()));
        }
        if(hasText(condition.getTeamName())){
            builder.and(teamDSL.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null){
            builder.and(memberDSL.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe() != null){
            builder.and(memberDSL.age.loe(condition.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        memberDSL.id.as("memberId"),
                        memberDSL.username,
                        memberDSL.age,
                        teamDSL.id.as("teamId"),
                        teamDSL.name.as("teamName")
                ))
                .from(memberDSL)
                .where(builder)
                .leftJoin(memberDSL.team, teamDSL)
                .fetch();
    }

    //where절 파라미터 동적 쿼리
    public List<MemberTeamDto> searchByWhere(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        memberDSL.id.as("memberId"),
                        memberDSL.username,
                        memberDSL.age,
                        teamDSL.id.as("teamId"),
                        teamDSL.name.as("teamName")
                ))
                .from(memberDSL)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .leftJoin(memberDSL.team, teamDSL)
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

}
