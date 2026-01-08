package ru.practicum.event.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.support.PageableExecutionUtils;
import ru.practicum.event.Event;
import ru.practicum.event.QEvent;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;


public class EventRepositoryImpl implements EventRepositoryCustom {

    private final QEvent qEvent = QEvent.event;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public EventRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<Event> getEventsPublic(PublicEventsParam publicEventsParam, Pageable pageable) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (publicEventsParam.getText() != null && !publicEventsParam.getText().isEmpty()) {
            predicate.and(qEvent.annotation.containsIgnoreCase(publicEventsParam.getText())
            .or(qEvent.description.containsIgnoreCase(publicEventsParam.getText())));
        }

//        if (email != null && !email.isEmpty()) {
//            predicate.and(qUser.email.containsIgnoreCase(email));
//        }
//
//        if (active != null) {
//            predicate.and(qUser.active.eq(active));
//        }

        List<Event> content = queryFactory
                .select(qEvent)
                .from(qEvent)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qEvent.eventDate.asc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qEvent.count())
                .from(qEvent)
                .where(predicate);

        return PageableExecutionUtils.getPage(content, pageable,
                countQuery::fetchOne);
    };
}
