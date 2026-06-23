# Stage 1: Build in the CI
# Stage 2: Optimizer for extracting layers
FROM public.ecr.aws/docker/library/amazoncorretto:25-alpine as optimizer

ARG APP_VERSION

WORKDIR /opt/beanbook

# Copy the JAR file from the CI build stage.
COPY build/libs/*-${APP_VERSION}.jar service.jar

# Extract the application's layers to optimize the final image.
# Managed by Spring Boot
RUN java -Djarmode=tools -jar service.jar extract --destination application
RUN java -XX:AOTCacheOutput=application/app.aot -Dspring.context.exit=onRefresh -Dspring.aot.enabled=true -jar application/service.jar

# Stage 3: Final
FROM public.ecr.aws/docker/library/amazoncorretto:25-alpine

ARG APP_VERSION
ENV SENTRY_RELEASE=${APP_VERSION}

# Expose the application port.
EXPOSE 5000

# Set the working directory for the final application.
WORKDIR /opt/beanbook

# Copy the cache from the optimizer stage.
COPY --from=optimizer /opt/beanbook/application ./

# Define the entry point to run the application, including a shared cache file for AOT Cache.
ENTRYPOINT exec java $JAVA_OPTS -Dspring.aot.enabled=true -XX:AOTCache=app.aot -jar service.jar
