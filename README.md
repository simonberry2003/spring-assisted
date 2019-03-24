Assisted Injection With Spring
==============================

Guava provides a mechanism to generate factories that produce objects where some parameters are only known at runtime.

This is an implementation for spring.

Example
=======

First define your factory and a class for the type it returns. It can also have an interface and must have a single public @Inject constructor.

```java
public interface PaymentFactory {
	Payment create(Date startDate, Date endDate, BigDecimal amount);
}
```

```java
public interface Payment {
}
```

```java
public class RealPayment implements Payment {

	private final CreditService creditService;
	private final Date startDate;
	private final Date endDate;
	private final BigDecimal amount;

	@Inject
	public RealPayment(CreditService creditService, @Assisted Date startDate, @Assisted Date endDate, @Assisted BigDecimal amount) {
		this.creditService = Preconditions.checkNotNull(creditService);
		this.startDate = Preconditions.checkNotNull(startDate);
		this.endDate = Preconditions.checkNotNull(endDate);
		this.amount = Preconditions.checkNotNull(amount);
	}
}
```

Then you need to inject AssistedFactoryProvider which creates an instance of your factory.

private @Inject AssistedFactoryProvider assistedFactoryProvider;

```java
@Bean
public PaymentFactory paymentFactory() {
	return assistedFactoryProvider.provide(PaymentFactory.class, RealPayment.class);
}
```

Define any services/components required that are non assisted.

```java
@Service
public class CreditService {
}
```

Then inject your payment factory.

```java
@Service
public class ExampleService {

	private @Inject PaymentFactory paymentFactory;

	@PostConstruct
	public void postConstruct() {
		Object payment = paymentFactory.create(new Date(0), new Date(), BigDecimal.ONE);
		System.out.println(payment);
	}
}
```
