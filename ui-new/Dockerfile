FROM node:20-alpine AS build

WORKDIR /app

RUN corepack enable
COPY package.json pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile

COPY . .

ARG VITE_API_URL=http://localhost:8762
ARG VITE_WS_URL=ws://localhost:8762/websocket
ARG VITE_USE_MOCK_API=false
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_USE_MOCK_API=$VITE_USE_MOCK_API

RUN pnpm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]