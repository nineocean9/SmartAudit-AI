FROM node:20-alpine AS builder
WORKDIR /build
RUN corepack enable && corepack prepare pnpm@11.9.0 --activate
COPY ruoyi-ui/package.json ruoyi-ui/pnpm-lock.yaml ruoyi-ui/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY ruoyi-ui/ .
RUN pnpm build:prod

FROM nginx:1.27-alpine
COPY --from=builder /build/dist/ /usr/share/nginx/html/
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
