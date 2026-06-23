# Stage 1: Build in the CI
# Stage 2: Optimizer for extracting layers
FROM public.ecr.aws/docker/library/amazoncorretto:25-alpine AS optimizer

ARG APP_VERSION

WORKDIR /opt/beanbook

# Copy the JAR file from the CI build stage.
COPY build/libs/*-${APP_VERSION}.jar service.jar

# Extract the application's layers to optimize the final image.
# Managed by Spring Boot
RUN java -Djarmode=tools -jar service.jar extract --destination application
# NOTE: no JDK AOT cache (Project Leyden) is trained here. A Leyden AOT cache bakes in CPU-specific
# compiled code (it records the instruction-set extensions of the machine that trained it). Since
# this image is built on a CI runner and run on arbitrary homelab CPUs, a baked-in cache can crash
# with SIGILL (illegal instruction) on a host whose CPU lacks those extensions. Portability wins
# over the small startup speedup; see the ENTRYPOINT below.

# Stage 3: Final
FROM public.ecr.aws/docker/library/amazoncorretto:25-alpine

ARG APP_VERSION
ENV SENTRY_RELEASE=${APP_VERSION}

# Expose the application port (matches server.port in application.yml).
EXPOSE 8001

# Set the working directory for the final application.
WORKDIR /opt/beanbook

# Copy the cache from the optimizer stage.
COPY --from=optimizer /opt/beanbook/application ./

# Run as a non-root user. /data holds the SQLite DB+WAL (mounted as a volume in compose.yaml);
# a fresh named volume inherits this ownership on first mount, so the app can write to it.
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data \
    && chown -R app:app /opt/beanbook /data
USER app

# Liveness probe — the app serves the SPA shell at / (Actuator is not on the classpath).
HEALTHCHECK --start-period=40s --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8001/ >/dev/null 2>&1 || exit 1

# Plain launch — no JDK AOT cache, so the JIT compiles for the actual runtime CPU and the image
# runs on any amd64 host. (A build-time Leyden cache trained on the CI runner's CPU can SIGILL on a
# homelab CPU with a narrower instruction set.) spring.aot.enabled is intentionally not set because
# the JAR was not built with Spring's processAot step.
ENTRYPOINT ["java", "-jar", "service.jar"]
