package com.example.querydsl_repeat;

import com.example.querydsl_repeat.entity.Member;
import com.example.querydsl_repeat.entity.QMember;
import com.example.querydsl_repeat.entity.QTeam;
import com.example.querydsl_repeat.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.example.querydsl_repeat.entity.QMember.*;
import static com.example.querydsl_repeat.entity.QTeam.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamB);
        em.persist(teamA);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        //given
        // TODO: member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {
        //given
        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl2() throws Exception {
        //given

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void search2() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10,30)))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"), (member.age.eq(10)))
                .fetchOne();
        //when

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetchTest() throws Exception {
        //given
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> content = results.getResults();

        queryFactory
                .selectFrom(member)
                .fetchCount();
        //when

        //then

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 3. 단 2에서 회원 이름이 없이면 마지막에 출력(nulls last),(nullFirst)도있음
     * @throws Exception
     */
    @Test
    public void sort() throws Exception {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        //then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();


    }

    @Test
    public void paging1() throws Exception {
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        //then
        assertThat(result.size()).isEqualTo(2);


    }

    @Test
    public void paging2() throws Exception {
        //when
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        //then
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);


    }

    @Test
    public void aggregation() throws Exception {
        //when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        //then
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    public void group() throws Exception {
        //when
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        //then
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     * @throws Exception
     */
    @Test
    public void join() throws Exception {
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 연관관계 없는 세타조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /**
     * 회원과 팀을 조인하면서, 팀이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m,t from member t left join m.team t on t.name = 'teamA';
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception {
        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


    }

    /**
     * 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * @throws Exception
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    public void fetchJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();

    }

    /**
     * 나이가 가장 많은 회원 조회
     * @throws Exception
     */
    @Test
    public void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     * @throws Exception
     */
    @Test
    public void subQuery2() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member3","member4");

        assertThat(result)
                .extracting("age")
                .containsExactly(30,40);
    }


    @Test
    public void subQuery3() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(20,30,40);
    }

    @Test
    public void subQuery4() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() throws Exception {
        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        //when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        //when
        //username_age
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
        
    }
}
