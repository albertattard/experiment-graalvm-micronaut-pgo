package demo.fst;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;

@ExecuteOn(TaskExecutors.IO)
@Controller("/quote")
public class QuoteController {

    private final QuoteRepository repository;

    public QuoteController(final QuoteRepository repository) {
        requireNonNull(repository);
        this.repository = repository;
    }

    @Get("/random")
    public HttpResponse<Quote> random() {
        final List<Quote> list = repository.findAll();

        if (list.isEmpty()) {
            return HttpResponse.notFound();
        }

        final Random random = new Random();
        final int index = random.nextInt(list.size());
        return HttpResponse.ok(list.get(index));
    }
}
