FROM node:24-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /app/dist /srv
EXPOSE 5173
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=20 CMD wget -qO- http://localhost:5173/ >/dev/null || exit 1
CMD ["caddy", "file-server", "--listen", ":5173", "--root", "/srv"]
