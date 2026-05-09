# --- Stage 1: build the Clojure uberjar -------------------------------------
FROM clojure:temurin-21-tools-deps-bookworm AS build
WORKDIR /app
COPY deps.edn build.clj ./
# Pre-fetch deps so the layer is cached when only sources change.
RUN clojure -P && clojure -A:build -P
COPY src ./src
COPY resources ./resources
RUN clojure -T:build uber

# --- Stage 2: runtime: JRE + Python + oemer ---------------------------------
FROM python:3.11-slim-bookworm AS runtime

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      openjdk-21-jre-headless \
      libgl1 libglib2.0-0 \
 && rm -rf /var/lib/apt/lists/*

# Install oemer and pin onnxruntime to a stable wheel.
# oemer pulls onnxruntime + opencv-python + numpy at install time.
RUN pip install --no-cache-dir oemer==0.1.7

# Pre-download oemer's models so the first request isn't slowed by a cold pull.
# `oemer --help` is enough to trigger the download in recent versions; if it
# isn't, the first /omr call will pay the one-time cost.
RUN oemer --help >/dev/null 2>&1 || true

WORKDIR /app
COPY --from=build /app/target/omr.jar /app/omr.jar

ENV PORT=8080 \
    OMR_ENGINE=oemer-local \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/omr.jar"]
