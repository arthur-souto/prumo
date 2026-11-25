package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.application.port.out.query.SortDirection;
import io.github.arthursouto.prumo.application.port.out.query.StorySearchCriteria;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryLabelEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserStoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserStorySearchRepository {

    private static final char ESCAPE = '\\';
    private static final String UNACCENT = "immutable_unaccent";
    private static final String PATTERN = "pattern";

    private final EntityManager entityManager;

    public UserStorySearchRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<UserStoryEntity> find(StorySearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserStoryEntity> query = builder.createQuery(UserStoryEntity.class);
        Root<UserStoryEntity> story = query.from(UserStoryEntity.class);

        query.select(story)
                .where(predicates(builder, query, story, criteria))
                .orderBy(order(builder, story, criteria));

        return bind(entityManager.createQuery(query), criteria)
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.size())
                .getResultList();
    }

    public long count(StorySearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<UserStoryEntity> story = query.from(UserStoryEntity.class);

        query.select(builder.count(story)).where(predicates(builder, query, story, criteria));

        return bind(entityManager.createQuery(query), criteria).getSingleResult();
    }

    private static <T> TypedQuery<T> bind(TypedQuery<T> query, StorySearchCriteria criteria) {
        if (criteria.text() != null) {
            query.setParameter(PATTERN, "%" + escape(criteria.text()) + "%");
        }
        return query;
    }

    private static Predicate[] predicates(CriteriaBuilder builder,
                                          CommonAbstractCriteria query,
                                          Root<UserStoryEntity> story,
                                          StorySearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(story.get("workspaceId"), criteria.workspaceId()));

        if (criteria.text() != null) {
            ParameterExpression<String> pattern = builder.parameter(String.class, PATTERN);
            Expression<String> unaccentedPattern = unaccent(builder, pattern);
            predicates.add(builder.or(
                    builder.like(unaccent(builder, builder.lower(story.get("title"))), unaccentedPattern, ESCAPE),
                    builder.like(builder.lower(story.get("storyKey")), pattern, ESCAPE),
                    builder.like(unaccent(builder, builder.lower(story.get("description"))), unaccentedPattern, ESCAPE)
            ));
        }
        if (!criteria.priorities().isEmpty()) {
            predicates.add(story.get("priority").in(criteria.priorities()));
        }
        if (!criteria.columnIds().isEmpty()) {
            predicates.add(story.get("columnId").in(criteria.columnIds()));
        }
        if (!criteria.labels().isEmpty()) {
            Subquery<Integer> labeled = query.subquery(Integer.class);
            Root<StoryLabelEntity> label = labeled.from(StoryLabelEntity.class);
            labeled.select(builder.literal(1)).where(
                    builder.equal(label.get("storyId"), story.get("id")),
                    builder.lower(label.get("label")).in(criteria.labels())
            );
            predicates.add(builder.exists(labeled));
        }

        List<Predicate> assignee = new ArrayList<>();
        if (!criteria.assigneeIds().isEmpty()) {
            assignee.add(story.get("assigneeId").in(criteria.assigneeIds()));
        }
        if (criteria.unassigned()) {
            assignee.add(builder.isNull(story.get("assigneeId")));
        }
        if (!assignee.isEmpty()) {
            predicates.add(builder.or(assignee.toArray(Predicate[]::new)));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private static List<Order> order(CriteriaBuilder builder, Root<UserStoryEntity> story, StorySearchCriteria criteria) {
        boolean ascending = criteria.direction() == SortDirection.ASC;
        Expression<Integer> number = story.get("number");
        Order byNumber = ascending ? builder.asc(number) : builder.desc(number);

        Expression<?> key = switch (criteria.sort()) {
            case NUMBER -> null;
            case UPDATED_AT -> story.get("updatedAt");
            case PRIORITY -> builder.<StoryPriority, Integer>selectCase(story.get("priority"))
                    .when(StoryPriority.LOW, 0)
                    .when(StoryPriority.MEDIUM, 1)
                    .when(StoryPriority.HIGH, 2)
                    .otherwise(3);
        };

        if (key == null) {
            return List.of(byNumber);
        }
        return List.of(ascending ? builder.asc(key) : builder.desc(key), byNumber);
    }

    private static Expression<String> unaccent(CriteriaBuilder builder, Expression<String> value) {
        return builder.function(UNACCENT, String.class, value);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
