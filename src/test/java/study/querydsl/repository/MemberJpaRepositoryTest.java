package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.MemberDSL;
import study.querydsl.entity.TeamDSL;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager entityManager;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){
        MemberDSL memberDSL = new MemberDSL("member1", 10);
        memberJpaRepository.save(memberDSL);
        //Optional인데 원래는 .get( ) 식으로 가져오는 건 지양. 테스트이므로 이렇게 그대로 진행
        MemberDSL findMember = memberJpaRepository.findById(memberDSL.getId()).get();
        assertThat(findMember).isEqualTo(memberDSL);

        List<MemberDSL> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(memberDSL);

        List<MemberDSL> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(memberDSL);
    }
    @Test
    public void basicQuerydslTest(){
        MemberDSL memberDSL = new MemberDSL("member1", 10);
        memberJpaRepository.save(memberDSL);

        List<MemberDSL> result1 = memberJpaRepository.findAll_Querydsl();
        assertThat(result1).containsExactly(memberDSL);

        List<MemberDSL> result2 = memberJpaRepository.findByUsername_QueryDSL("member1");
        assertThat(result2).containsExactly(memberDSL);
    }
    @Test
    public void searchTest(){
        TeamDSL teamA = new TeamDSL("teamA");
        TeamDSL teamB = new TeamDSL("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        MemberDSL member1 = new MemberDSL("member1", 10, teamA);
        MemberDSL member2 = new MemberDSL("member2", 20, teamA);
        MemberDSL member3 = new MemberDSL("member3", 30, teamB);
        MemberDSL member4 = new MemberDSL("member4", 40, teamB);
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByWhere(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }
}