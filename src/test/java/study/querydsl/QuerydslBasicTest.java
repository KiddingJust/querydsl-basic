package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.MemberDSL;
import study.querydsl.entity.QMemberDSL;
import study.querydsl.entity.TeamDSL;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMemberDSL.*;
import static study.querydsl.entity.QTeamDSL.teamDSL;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라.
        MemberDSL findMember =
                em.createQuery(
                        "select m from MemberDSL m " +
                                "where m.username = :username", MemberDSL.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void startQuerydsl(){
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        //QMemberDSL 안뜨면, Gradle 들어가서 others>compileQuerydsl 해줘. 

//        QMemberDSL m = new QMemberDSL("m");
        QMemberDSL m = memberDSL;
        //==> QMemberDSL.memberDSL은 static import로 memberDSL 로만 써도 됨.
        //==> 그럼 아래 m 들은 모두 memberDSL 로 변경 가능. 훨씬 깔끔해짐.
        MemberDSL findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))    //파라미터 바인딩을 따로 해줄 필요 없음.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        MemberDSL findMember = queryFactory
                .selectFrom(memberDSL)
                .where(memberDSL.username.eq("member1")
                        .and(memberDSL.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void search2(){
        MemberDSL findMember = queryFactory
                .selectFrom(memberDSL)
                .where(memberDSL.username.eq("member1"), memberDSL.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 오름차순
     * 단 2에서 회원 이름 없으면 마지막에 출력
     */
    @Test
    public void sort(){
        //데이터 추가
        em.persist(new MemberDSL(null, 100));
        em.persist(new MemberDSL("member5", 100));
        em.persist(new MemberDSL("member6", 100));

        List<MemberDSL> results = queryFactory
                .selectFrom(memberDSL)
                .where(memberDSL.age.eq(100))
                .orderBy(memberDSL.age.desc()
                        , memberDSL.username.asc().nullsLast())
                .fetch();
        MemberDSL member5 = results.get(0);
        MemberDSL member6 = results.get(1);
        MemberDSL memberNull = results.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging(){
        QueryResults<MemberDSL> queryResults = queryFactory
                .selectFrom(memberDSL)
                .orderBy(memberDSL.username.desc())
                .offset(1)
                .limit(2)
                //deprecated ==> count query 따로 날려야할듯.
                .fetchResults();
    }

    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(teamDSL.name, memberDSL.age.avg())
                .from(memberDSL)
                .join(memberDSL.team, teamDSL)
                .groupBy(teamDSL.name)  //team 이름으로 그룹핑
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(teamDSL.name)).isEqualTo("teamA");
        assertThat(teamA.get(memberDSL.age.avg())).isEqualTo(15);   //10, 20 의 평균이므로
    }

    @Test
    public void join(){
        List<MemberDSL> result = queryFactory
                .selectFrom(memberDSL)
                .join(memberDSL.team, teamDSL)
                .where(teamDSL.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 회원과 팀 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.team = 'teamA'
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(memberDSL, teamDSL)
                .from(memberDSL)
                .leftJoin(memberDSL.team, teamDSL)
                .on(teamDSL.name.eq("teamA"))
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username=t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t on m.username=t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //이름이 teamA, teamB인 Member를 새로 생성
        em.persist(new MemberDSL("teamA"));
        em.persist(new MemberDSL("teamB"));

        List<Tuple> result = queryFactory
                .select(memberDSL, teamDSL)
                .from(memberDSL)
                //보통은 join(member.team, team) 등으로 조인하는데 그건 id값으로 조인하는 것
                .leftJoin(teamDSL).on(memberDSL.username.eq(teamDSL.name))
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();
        MemberDSL findMember = queryFactory
                    .selectFrom(memberDSL)
                    .where(memberDSL.username.eq("member1"))
                    .fetchOne();

        //로딩된 엔티티인지, 초기화된 엔티티인지 명확하게 알려줌.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }
    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();
        MemberDSL findMember = queryFactory
                .selectFrom(memberDSL)
                .join(memberDSL.team, teamDSL).fetchJoin()  //fetchJoin() 만 추가하면 됨.
                .where(memberDSL.username.eq("member1"))
                .fetchOne();

        //로딩된 엔티티인지, 초기화된 엔티티인지 명확하게 알려줌.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        //fetch join이므로 teamDSL 객체도 가지고 있음.
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }
}
