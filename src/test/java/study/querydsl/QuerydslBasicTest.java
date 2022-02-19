package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
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

    /**
     * 1. 나이가 가장 많은 회원 조회
     * 2. 나이가 평균 이상인 회원도 비슷하게 조회 가능.
     * 3. IN쿼리도, eq를 in으로 바꾸어주기만 하면 됨.
     */
    @Test
    public void subQuery(){
        //alias 겹치면 안되므로 하나 더 만들어줌. 서브쿼리용
        QMemberDSL memberSub = new QMemberDSL("memberSub");

        List<MemberDSL> result = queryFactory
                .selectFrom(memberDSL)
                .where(memberDSL.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     * select 서브쿼리
     */
    @Test
    public void selectSubQuery(){
        QMemberDSL memberSub = new QMemberDSL("memberSub");
        List<Tuple> result = queryFactory
                .select(memberDSL.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(memberDSL)
                .fetch();
    }
    /**
     * case문 간단버전과 복잡한 버전.
     */
    @Test
    public void complexCase(){
        //simple
        queryFactory
                .select(memberDSL.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(memberDSL)
                .fetch();

        //complex
        queryFactory
                .select(new CaseBuilder()
                        .when(memberDSL.age.between(0, 20)).then("주니어")
                        .when(memberDSL.age.between(21, 40)).then("시니어")
                        .otherwise("올드맨"))
                .from(memberDSL)
                .fetch();
    }

    //프로젝션 대상 하나일 때
    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(memberDSL.username)
                .from(memberDSL)
                .fetch();
        //참고로 아래와 같은 형태도 프로젝션 대상이 하나인 것. 객체 하나.
        List<MemberDSL> fetch2 = queryFactory
                .select(memberDSL)
                .from(memberDSL)
                .fetch();
    }
    // 튜플 프로젝션
    @Test
    public void tupleProjection(){
        //memberDSL을 통으로 가져오는 게 아니라, 이 두개 데이터만 필요.
        //이렇게 두개 이상을 꺼내서 받을 땐 튜플로 꺼내야함.
        List<Tuple> result = queryFactory
                .select(memberDSL.username, memberDSL.age)
                .from(memberDSL)
                .fetch();
        for(Tuple tuple : result){
            String username = tuple.get(memberDSL.username);
            Integer age = tuple.get(memberDSL.age);
            System.out.println(username + " : " + age);
        }
    }
    //순수 JPA DTO
    @Test
    public void findDtoByJPQL(){
        //dto 패키지 다 적어주어야 해서 지저분하고,
        //생성자 방식만 지원함.
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from MemberDSL m", MemberDto.class)
                .getResultList();

        for(MemberDto m : result){
            System.out.println("memberDto = " + m);
        }
    }

    @Test
    public void findDtoBySetter(){
        //new instance 등으로 만들어주어야 하므로, 기본 생성자도 있어야함.
        //MemberDto에 @NoArgsConstructor 추가
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        memberDSL.username,
                        memberDSL.age))
                .from(memberDSL)
                .fetch();
        for(MemberDto m : result){
            System.out.println("memberDto = " + m);
        }
    }
    @Test
    public void findDtoByField(){
        //MemberDto에 getter, setter가 없어도 됨. 즉 지금 해둔 @Data를 빼도 됨.
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        memberDSL.username,
                        memberDSL.age))
                .from(memberDSL)
                .fetch();
        for(MemberDto m : result){
            System.out.println("memberDto = " + m);
        }
    }
    @Test
    public void findDtoByConstructor(){
        //MemberDto에 getter, setter가 없어도 됨. 즉 지금 해둔 @Data를 빼도 됨.
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        memberDSL.username,
                        memberDSL.age))
                .from(memberDSL)
                .fetch();
        for(MemberDto m : result){
            System.out.println("memberDto = " + m);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(memberDSL.username, memberDSL.age))
                .from(memberDSL)
                .fetch();
        for(MemberDto m : result){
            System.out.println("memberDto = " + m);
        }
    }
    // 동적 쿼리
    // 1. booleanbuilder
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<MemberDSL> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<MemberDSL> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            //builder에 and조건 추가
            builder.and(memberDSL.username.eq(usernameParam));
        }
        if(ageParam != null){
            builder.and(memberDSL.age.eq(ageParam));
        }
        return queryFactory
                .selectFrom(memberDSL)
                .where(builder)
                .fetch();
    }
    //2. where 다중 파라미터
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<MemberDSL> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<MemberDSL> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(memberDSL)
//                .where(usernameEq(usernameParam), ageEq(ageParam))
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }
    private Predicate usernameEq(String usernameParam) {
        if(usernameParam != null){
            return memberDSL.username.eq(usernameParam);
        }else{
            return null;
        }
    }
    private Predicate ageEq(Integer ageParam) {
        return ageParam != null ? memberDSL.age.eq(ageParam) : null;
    }
    //그리고 조립도 가능하다. usernameEq, ageEq를 그대로 조립
    private BooleanExpression usernameEq2(String usernameParam) {
        return usernameParam != null ? memberDSL.username.eq(usernameParam) : null;
    }
    private BooleanExpression ageEq2(Integer ageParam) {
        return ageParam != null ? memberDSL.age.eq(ageParam) : null;
    }
    private BooleanExpression allEq(String usernameParam, Integer ageParam){
        return usernameEq2(usernameParam).and(ageEq2(ageParam));
    }

    //벌크 쿼리
    @Test
    public void bulkUpdate(){
        /**
         * 영속성 컨텍스트
         * member1, member2, member3, member4
         * --> 연산 후에는 비회원, 비회원, member3, member4가 되는 게 정상.
         * --> 그런데 벌크 연산은 영속성 컨텍스트를 무시함. 바로 DB에 쿼리를 날리는 것
         */
        long count = queryFactory
                .update(memberDSL)
                .set(memberDSL.username, "비회원")
                .where(memberDSL.age.lt(28))
                .execute();
        /**
         * select 해오면 반영되지 않음. 하지만 DB 값은 변경됨
         * 이는 영속성 컨텍스트의 값을 우선적으로 하기 때문. --> repeatable read
         *         em.flush();
         *         em.clear(); 해주면 됨.
         */

        List<MemberDSL> result = queryFactory
                .selectFrom(memberDSL)
                .fetch();
        result.stream().forEach(m -> System.out.println(m));
    }
    @Test
    public void bulkAddAndMultiply(){
        queryFactory
                .update(memberDSL)
//                .set(memberDSL.age, memberDSL.age.add(1))
                .set(memberDSL.age, memberDSL.age.multiply(2))
                .execute();
    }
    @Test
    public void bulkDelete(){
        queryFactory
                .delete(memberDSL)
                .where(memberDSL.age.gt(18))
                .execute();
    }

    //실패하는데 이유를 모르겠음. D
    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})"
                        , memberDSL.username,
                        "member", "M"))
                .from(memberDSL)
                .fetch();
        for(String s : result){
            System.out.println("s = " + s);
        }
    }

}
