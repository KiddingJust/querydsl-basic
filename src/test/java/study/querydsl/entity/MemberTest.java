package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity(){
        TeamDSL teamA = new TeamDSL("teamA");
        TeamDSL teamB = new TeamDSL("teamB");
        em.persist(teamA);
        em.persist(teamB);

        MemberDSL member1 = new MemberDSL("member1", 10, teamA);
        MemberDSL member2 = new MemberDSL("member2", 20, teamA);
        MemberDSL member3 = new MemberDSL("member3", 30, teamB);
        MemberDSL member4 = new MemberDSL("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화 - 영속성 컨텍스트 DB에 날리고, 캐시 지워버림.
        em.flush();
        em.clear();

        //확인
        List<MemberDSL> members = em.createQuery("select m from MemberDSL m", MemberDSL.class)
                .getResultList();

        for (MemberDSL member:members){
            System.out.println("member = " + member);
            System.out.println("-> member.team " + member.getTeam());
        }
    }

}