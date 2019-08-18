---
layout: default
title: Fallback
---

# Fallback

[Fallbacks][Fallback] allow you to provide an alternative result for a failed execution. They can also be used to suppress exceptions and provide a default result:

```java
Fallback<Object> fallback = Fallback.of(null);
```

Throw a custom exception:

```java
Fallback<Object> fallback = Fallback.ofException(e -> new CustomException(e.getLastFailure()));
```

Or compute an alternative result such as from a backup resource:

```java
Fallback<Object> fallback = Fallback.of(this::connectToBackup);
```

For computations that block, a Fallback can be configured to run asynchronously:

```java
Fallback<Object> fallback = Fallback.ofAsync(this::blockingCall);
```

And like any [FailurePolicy], Fallbacks can be configured to handle only [certain results or failures][failure-handling]:

```java
fallback
  .handle(ConnectException.class)
  .handleResult(null);
```

{% include common-links.html %}