{{/*
Expand the name of the chart.
*/}}
{{- define "order-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "order-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "order-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "order-platform.labels" -}}
helm.sh/chart: {{ include "order-platform.chart" . }}
{{ include "order-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "order-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "order-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "order-platform.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "order-platform.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Get image tag - uses specific tag if set, otherwise global tag
*/}}
{{- define "order-platform.imageTag" -}}
{{- $tag := .imageTag | default .globalTag | default "latest" }}
{{- $tag }}
{{- end }}

{{/*
Create image reference
*/}}
{{- define "order-platform.image" -}}
{{- $registry := .registry }}
{{- $repository := .repository }}
{{- $tag := include "order-platform.imageTag" (dict "imageTag" .tag "globalTag" .globalTag) }}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- end }}

{{/*
PostgreSQL connection string
*/}}
{{- define "order-platform.postgresHost" -}}
{{- printf "%s-postgresql" .Release.Name }}
{{- end }}

{{/*
MongoDB connection string
*/}}
{{- define "order-platform.mongoHost" -}}
{{- printf "%s-mongodb" .Release.Name }}
{{- end }}

{{/*
Redis connection string
*/}}
{{- define "order-platform.redisHost" -}}
{{- printf "%s-redis-master" .Release.Name }}
{{- end }}

{{/*
Kafka bootstrap servers
*/}}
{{- define "order-platform.kafkaBootstrapServers" -}}
{{- printf "%s-kafka:9092" .Release.Name }}
{{- end }}

{{/*
Common environment variables for all services
*/}}
{{- define "order-platform.commonEnv" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "k8s"
- name: SPRING_KAFKA_BOOTSTRAP_SERVERS
  value: {{ include "order-platform.kafkaBootstrapServers" . | quote }}
- name: SPRING_DATA_REDIS_HOST
  value: {{ include "order-platform.redisHost" . | quote }}
- name: SPRING_DATA_REDIS_PORT
  value: "6379"
- name: JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "order-platform.fullname" . }}-secrets
      key: jwt-secret
{{- end }}

{{/*
PostgreSQL environment variables
*/}}
{{- define "order-platform.postgresEnv" -}}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ include "order-platform.postgresHost" . }}:5432/{{ .dbName }}"
- name: SPRING_DATASOURCE_USERNAME
  value: "postgres"
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "order-platform.fullname" . }}-secrets
      key: postgres-password
{{- end }}

{{/*
MongoDB environment variables
*/}}
{{- define "order-platform.mongoEnv" -}}
- name: SPRING_DATA_MONGODB_URI
  value: "mongodb://{{ include "order-platform.mongoHost" . }}:27017/{{ .dbName }}"
{{- end }}

{{/*
Liveness probe
*/}}
{{- define "order-platform.livenessProbe" -}}
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: http
  initialDelaySeconds: {{ .initialDelaySeconds | default 60 }}
  periodSeconds: {{ .periodSeconds | default 10 }}
  timeoutSeconds: {{ .timeoutSeconds | default 5 }}
  failureThreshold: {{ .failureThreshold | default 3 }}
{{- end }}

{{/*
Readiness probe
*/}}
{{- define "order-platform.readinessProbe" -}}
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: http
  initialDelaySeconds: {{ .initialDelaySeconds | default 30 }}
  periodSeconds: {{ .periodSeconds | default 5 }}
  timeoutSeconds: {{ .timeoutSeconds | default 3 }}
  failureThreshold: {{ .failureThreshold | default 3 }}
{{- end }}

