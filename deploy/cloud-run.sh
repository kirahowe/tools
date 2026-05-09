#!/usr/bin/env bash
# Deploy the OMR backend to Cloud Run.
#
# Prereqs:
#   - gcloud CLI installed and authenticated (`gcloud auth login`)
#   - Project selected (`gcloud config set project <PROJECT_ID>`)
#   - Artifact Registry API + Cloud Run API enabled
#
# Usage:
#   PROJECT_ID=my-project REGION=us-central1 ./deploy/cloud-run.sh
#
# The container is built remotely with Cloud Build, so you don't need a
# local Docker daemon.

set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID env var}"
REGION="${REGION:-us-central1}"
SERVICE="${SERVICE:-sheet-music-omr}"
REPO="${REPO:-omr}"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${SERVICE}:latest"

# Create the Artifact Registry repo if it doesn't exist (idempotent-ish).
gcloud artifacts repositories describe "${REPO}" --location="${REGION}" \
  --project="${PROJECT_ID}" >/dev/null 2>&1 || \
  gcloud artifacts repositories create "${REPO}" \
    --repository-format=docker \
    --location="${REGION}" \
    --project="${PROJECT_ID}"

# Build the image with Cloud Build (no local Docker required).
gcloud builds submit ./backend \
  --tag "${IMAGE}" \
  --project "${PROJECT_ID}"

# Deploy / update the Cloud Run service. Scale-to-zero by default.
# Memory is 4Gi because oemer's TF/ONNX runtime is hungry; CPU is 2 for speed.
gcloud run deploy "${SERVICE}" \
  --image "${IMAGE}" \
  --project "${PROJECT_ID}" \
  --region "${REGION}" \
  --platform managed \
  --allow-unauthenticated \
  --memory 4Gi \
  --cpu 2 \
  --timeout 600 \
  --concurrency 1 \
  --min-instances 0 \
  --max-instances 3 \
  --set-env-vars OMR_ENGINE=oemer-local

URL=$(gcloud run services describe "${SERVICE}" \
        --region "${REGION}" --project "${PROJECT_ID}" \
        --format='value(status.url)')

echo
echo "Deployed: ${URL}"
echo "Health:    ${URL}/health"
echo
echo "Set this in web/config.js (or window.OMR_API_URL on the page) before"
echo "publishing the static frontend:"
echo "  export const API_URL = \"${URL}\";"
