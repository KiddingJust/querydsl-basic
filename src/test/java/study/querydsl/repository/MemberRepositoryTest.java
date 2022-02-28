package study.querydsl.repository;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager entityManager;
    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest(){
        MemberDSL memberDSL = new MemberDSL("member1", 10);
        memberRepository.save(memberDSL);
        //Optional인데 원래는 .get( ) 식으로 가져오는 건 지양. 테스트이므로 이렇게 그대로 진행
        MemberDSL findMember = memberRepository.findById(memberDSL.getId()).get();
        assertThat(findMember).isEqualTo(memberDSL);

        List<MemberDSL> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(memberDSL);

        List<MemberDSL> result2 = memberRepository.findByUsername("member1");
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

        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }
}