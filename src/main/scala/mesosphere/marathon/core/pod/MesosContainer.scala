package mesosphere.marathon.core.pod

import mesosphere.marathon.raml.{
  Image,
  Endpoint,
  Resources,
  MesosExec,
  HealthCheck,
  VolumeMount,
  Artifact,
  Lifecycle,
  PodContainer,
  EnvVars,
  KVLabels
}
import mesosphere.marathon.state
import mesosphere.marathon.plugin.ContainerSpec
import mesosphere.marathon.raml
import scala.collection.immutable.Map

case class MesosContainer(
  name: String,
  exec: Option[MesosExec] = None,
  resources: Resources,
  endpoints: scala.collection.immutable.Seq[Endpoint] = Nil,
  image: Option[Image] = None,
  env: Map[String, state.EnvVarValue] = Map.empty,
  user: Option[String] = None,
  healthCheck: Option[HealthCheck] = None, //TODO(PODS): use health.HealthCheck
  volumeMounts: scala.collection.immutable.Seq[VolumeMount] = Nil,
  artifacts: scala.collection.immutable.Seq[Artifact] = Nil, //TODO(PODS): use FetchUri
  labels: Map[String, String] = Map.empty,
  lifecycle: Option[Lifecycle] = None) extends ContainerSpec

object MesosContainer {

  //TODO(PODS): find a better place for this converters (should probably live in the API)

  def toStateEnv(envVars: EnvVars): Map[String, state.EnvVarValue] = {
    def toEnvValue(envVarValue: raml.EnvVarValueOrSecret): state.EnvVarValue = envVarValue match {
      case raml.EnvVarValue(value) => state.EnvVarString(value)
      case raml.EnvVarSecretRef(secret) => state.EnvVarSecretRef(secret)
    }
    envVars.values.map{ case (k, v) => k -> toEnvValue(v) }
  }

  def toRamlEnv(envMap: Map[String, state.EnvVarValue]): EnvVars = {
    def toEnvValue(env: state.EnvVarValue): raml.EnvVarValueOrSecret = env match {
      case state.EnvVarString(value) => raml.EnvVarValue(value)
      case state.EnvVarSecretRef(secret) => raml.EnvVarSecretRef(secret)
    }
    EnvVars(envMap.map { case (k, v) => k -> toEnvValue(v) })
  }

  def apply(c: PodContainer): MesosContainer = MesosContainer(
    name = c.name,
    exec = c.exec,
    resources = c.resources,
    endpoints = c.endpoints,
    image = c.image,
    env = c.environment.fold(Map.empty[String, state.EnvVarValue])(toStateEnv),
    user = c.user,
    healthCheck = c.healthCheck,
    volumeMounts = c.volumeMounts,
    artifacts = c.artifacts,
    labels = c.labels.fold(Map.empty[String, String])(_.values),
    lifecycle = c.lifecycle
  )

  def toPodContainer(c: MesosContainer): PodContainer = PodContainer(
    name = c.name,
    exec = c.exec,
    resources = c.resources,
    endpoints = c.endpoints,
    image = c.image,
    environment = if (c.env.isEmpty) None else Some(toRamlEnv(c.env)),
    user = c.user,
    healthCheck = c.healthCheck,
    volumeMounts = c.volumeMounts,
    artifacts = c.artifacts,
    labels = if (c.labels.isEmpty) None else Some(KVLabels(c.labels)),
    lifecycle = c.lifecycle
  )
}
