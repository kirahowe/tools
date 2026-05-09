// Edit this for production. Set the URL of your deployed Cloud Run / Fly / VM.
// Local dev defaults to the docker-compose backend on :8080.
export const API_URL =
  (typeof window !== "undefined" && window.OMR_API_URL) ||
  "http://localhost:8080";
