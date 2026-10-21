# 构建阶段（Vite 6 要求 Node ≥ 20.19）
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
# 2G 内存小机型：放宽 Node 构建堆上限，避免 vite build 打包 OOM
ENV NODE_OPTIONS="--max-old-space-size=1280"
COPY . .
# 同源部署：VITE_BASE_URL 留空（由 nginx 反代后端，无跨域/混合内容问题）
ARG VITE_BASE_URL=
ENV VITE_BASE_URL=$VITE_BASE_URL
ARG VITE_PUBLIC_PATH=/
ENV VITE_PUBLIC_PATH=$VITE_PUBLIC_PATH
RUN npm run build

# 运行阶段
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
