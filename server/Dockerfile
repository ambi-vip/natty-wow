# 定义应用程序名称和工作目录
ARG APP_NAME=server
ARG WORK_HOME=/opt/${APP_NAME}

# 使用基础镜像
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:21-ubuntu AS base

# 构建阶段
FROM base AS build
ARG WORK_HOME
ARG APP_NAME

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 设置工作目录
WORKDIR ${WORK_HOME}

# 拷贝并安装应用
COPY build/install/${APP_NAME} .

# 运行阶段
FROM base AS run
ARG WORK_HOME

# 从构建阶段复制应用程序到运行阶段
COPY --from=build ${WORK_HOME} ${WORK_HOME}

# 设置工作目录
WORKDIR ${WORK_HOME}

# 暴露服务端口
EXPOSE 8080

# 定义容器启动时执行的命令
ENTRYPOINT ["bin/server"]
