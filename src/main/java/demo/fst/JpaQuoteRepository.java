package demo.fst;

import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Singleton;

import javax.persistence.EntityManager;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Singleton
public class JpaQuoteRepository implements QuoteRepository {

    private final EntityManager entityManager;
    private final ApplicationConfiguration applicationConfiguration;

    public JpaQuoteRepository(final EntityManager entityManager,
                              final ApplicationConfiguration applicationConfiguration) {
        requireNonNull(entityManager);
        requireNonNull(applicationConfiguration);

        this.entityManager = entityManager;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    @ReadOnly
    public List<Quote> findAll() {
        final String qlString = "SELECT q FROM Quote as q";
        return entityManager.createQuery(qlString, Quote.class).getResultList();
    }
}
