// Copyright 2018, 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.validation.Valid;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Affinity;
import io.kubernetes.client.models.V1Capabilities;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1HostPathVolumeSource;
import io.kubernetes.client.models.V1NodeAffinity;
import io.kubernetes.client.models.V1NodeSelector;
import io.kubernetes.client.models.V1PodAffinity;
import io.kubernetes.client.models.V1PodAntiAffinity;
import io.kubernetes.client.models.V1PodReadinessGate;
import io.kubernetes.client.models.V1PodSecurityContext;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecurityContext;
import io.kubernetes.client.models.V1Toleration;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import oracle.kubernetes.json.Description;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static java.util.Collections.emptyList;

@Description("ServerPod describes the configuration for a Kubernetes pod for a server.")
class ServerPod extends KubernetesResource {

  private static final Comparator<V1EnvVar> ENV_VAR_COMPARATOR =
      Comparator.comparing(V1EnvVar::getName);
  private static final Comparator<V1Volume> VOLUME_COMPARATOR =
      Comparator.comparing(V1Volume::getName);
  private static final Comparator<V1VolumeMount> VOLUME_MOUNT_COMPARATOR =
      Comparator.comparing(V1VolumeMount::getName);

  /**
   * Environment variables to pass while starting a server.
   *
   * @since 2.0
   */
  @Valid
  @Description("A list of environment variables to add to a server.")
  private List<V1EnvVar> env = new ArrayList<>();

  /**
   * Defines the settings for the liveness probe. Any that are not specified will default to the
   * runtime liveness probe tuning settings.
   *
   * @since 2.0
   */
  @Description("Settings for the liveness probe associated with a server.")
  private ProbeTuning livenessProbe = new ProbeTuning();

  /**
   * Defines the settings for the readiness probe. Any that are not specified will default to the
   * runtime readiness probe tuning settings.
   *
   * @since 2.0
   */
  @Description("Settings for the readiness probe associated with a server.")
  private ProbeTuning readinessProbe = new ProbeTuning();

  /**
   * Defines the key-value pairs for the pod to fit on a node, the node must have each of the
   * indicated key-value pairs as labels.
   *
   * @since 2.0
   */
  @Description(
      "Selector which must match a node's labels for the pod to be scheduled on that node.")
  private Map<String, String> nodeSelector = new HashMap<>();

  @Description("If specified, the pod's scheduling constraints")
  private V1Affinity affinity = null;

  @Description("If specified, indicates the pod's priority. \"system-node-critical\" and \"system-cluster-critical\" "
      + "are two special keywords which indicate the highest priorities with the former being the highest priority. "
      + "Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod "
      + "priority will be default or zero if there is no default.")
  private String priorityClassName = null;

  @Description("If specified, all readiness gates will be evaluated for pod readiness. A pod is ready when all its "
      + "containers are ready AND all conditions specified in the readiness gates have status equal to \"True\" More "
      + "info: https://github.com/kubernetes/community/blob/master/keps/sig-network/0007-pod-ready%2B%2B.md")
  private List<V1PodReadinessGate> readinessGates = new ArrayList<>();

  @Description("Restart policy for all containers within the pod. One of Always, OnFailure, Never. Default to Always. "
      + "More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#restart-policy")
  private String restartPolicy = null;

  @Description("RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used to run "
      + "this pod.  If no RuntimeClass resource matches the named class, the pod will not be run. If unset or empty, "
      + "the \"legacy\" RuntimeClass will be used, which is an implicit class with an empty definition that uses the "
      + "default runtime handler. More "
      + "info: https://github.com/kubernetes/community/blob/master/keps/sig-node/0014-runtime-class.md This is an "
      + "alpha feature and may change in the future.")
  private String runtimeClassName = null;

  @Description("NodeName is a request to schedule this pod onto a specific node. If it is non-empty, the scheduler "
      + "simply schedules this pod onto that node, assuming that it fits resource requirements.")
  private String nodeName = null;

  @Description("If specified, the pod will be dispatched by specified scheduler. If not specified, the pod will be "
      + "dispatched by default scheduler.")
  private String schedulerName = null;

  @Description("If specified, the pod's tolerations.")
  private List<V1Toleration> tolerations = new ArrayList<>();

  /**
   * Defines the requirements and limits for the pod server.
   *
   * @since 2.0
   */
  @Description("Memory and CPU minimum requirements and limits for the server.")
  private V1ResourceRequirements resources =
      new V1ResourceRequirements().limits(new HashMap<>()).requests(new HashMap<>());

  /**
   * PodSecurityContext holds pod-level security attributes and common container settings. Some
   * fields are also present in container.securityContext. Field values of container.securityContext
   * take precedence over field values of PodSecurityContext.
   *
   * @since 2.0
   */
  @Description("Pod-level security attributes.")
  private V1PodSecurityContext podSecurityContext = new V1PodSecurityContext();

  /**
   * InitContainers holds a list of initialization containers that should be run before starting the
   * main containers in this pod.
   *
   * @since 2.1
   */
  @Description("Initialization containers to be included in the server pod.")
  private List<V1Container> initContainers = new ArrayList<>();

  /**
   * The additional containers.
   *
   * @since 2.1
   */
  @Description("Additional containers to be included in the server pod.")
  private List<V1Container> containers = new ArrayList<>();

  /**
   * Configures how the operator should shutdown the server instance.
   *
   * @since 2.2
   */
  @Description("Configures how the operator should shutdown the server instance.")
  private Shutdown shutdown = new Shutdown();

  /**
   * SecurityContext holds security configuration that will be applied to a container. Some fields
   * are present in both SecurityContext and PodSecurityContext. When both are set, the values in
   * SecurityContext take precedence
   *
   * @since 2.0
   */
  @Description(
      "Container-level security attributes. Will override any matching pod-level attributes.")
  private V1SecurityContext containerSecurityContext = new V1SecurityContext();

  /**
   * The additional volumes.
   *
   * @since 2.0
   */
  @Description("Additional volumes to be created in the server pod.")
  private List<V1Volume> volumes = new ArrayList<>();

  /**
   * The additional volume mounts.
   *
   * @since 2.0
   */
  @Description("Additional volume mounts for the server pod.")
  private List<V1VolumeMount> volumeMounts = new ArrayList<>();

  private static void copyValues(V1ResourceRequirements to, V1ResourceRequirements from) {
    if (from != null) {
      if (from.getRequests() != null) {
        from.getRequests().forEach(to.getRequests()::putIfAbsent);
      }
      if (from.getLimits() != null) {
        from.getLimits().forEach(to.getLimits()::putIfAbsent);
      }
    }
  }

  @SuppressWarnings("Duplicates")
  private void copyValues(V1PodSecurityContext to, V1PodSecurityContext from) {
    if (to.isRunAsNonRoot() == null) {
      to.runAsNonRoot(from.isRunAsNonRoot());
    }
    if (to.getFsGroup() == null) {
      to.fsGroup(from.getFsGroup());
    }
    if (to.getRunAsGroup() == null) {
      to.runAsGroup(from.getRunAsGroup());
    }
    if (to.getRunAsUser() == null) {
      to.runAsUser(from.getRunAsUser());
    }
    if (to.getSeLinuxOptions() == null) {
      to.seLinuxOptions(from.getSeLinuxOptions());
    }
    if (to.getSupplementalGroups() == null) {
      to.supplementalGroups(from.getSupplementalGroups());
    }
    if (to.getSysctls() == null) {
      to.sysctls(from.getSysctls());
    }
  }

  @SuppressWarnings("Duplicates")
  private void copyValues(V1SecurityContext to, V1SecurityContext from) {
    if (to.isAllowPrivilegeEscalation() == null) {
      to.allowPrivilegeEscalation(from.isAllowPrivilegeEscalation());
    }
    if (to.isPrivileged() == null) {
      to.privileged(from.isPrivileged());
    }
    if (to.isReadOnlyRootFilesystem() == null) {
      to.readOnlyRootFilesystem(from.isReadOnlyRootFilesystem());
    }
    if (to.isRunAsNonRoot() == null) {
      to.runAsNonRoot(from.isRunAsNonRoot());
    }
    if (to.getCapabilities() == null) {
      to.setCapabilities(from.getCapabilities());
    } else {
      copyValues(to.getCapabilities(), from.getCapabilities());
    }
    if (to.getRunAsGroup() == null) {
      to.runAsGroup(from.getRunAsGroup());
    }
    if (to.getRunAsUser() == null) {
      to.runAsUser(from.getRunAsUser());
    }
    if (to.getSeLinuxOptions() == null) {
      to.seLinuxOptions(from.getSeLinuxOptions());
    }
  }

  private void copyValues(V1Capabilities to, V1Capabilities from) {
    if (from.getAdd() != null) {
      List<String> allAddCapabilities = new ArrayList<>();
      if (to.getAdd() != null) {
        allAddCapabilities =
            Stream.concat(to.getAdd().stream(), from.getAdd().stream())
                .distinct()
                .collect(Collectors.toList());
      }
      to.setAdd(allAddCapabilities);
    }

    if (from.getDrop() != null) {
      List<String> allDropCapabilities = new ArrayList<>();
      if (to.getDrop() != null) {
        allDropCapabilities =
            Stream.concat(to.getDrop().stream(), from.getDrop().stream())
                .distinct()
                .collect(Collectors.toList());
      }
      to.setDrop(allDropCapabilities);
    }
  }

  private void copyValues(V1Affinity to, V1Affinity from) {
    if (to.getNodeAffinity() == null) {
      to.setNodeAffinity(from.getNodeAffinity());
    } else if (from.getNodeAffinity() != null) {
      copyValues(to.getNodeAffinity(), from.getNodeAffinity());
    }
    if (to.getPodAffinity() == null) {
      to.setPodAffinity(from.getPodAffinity());
    } else if (from.getPodAffinity() != null) {
      copyValues(to.getPodAffinity(), from.getPodAffinity());
    }
    if (to.getPodAntiAffinity() == null) {
      to.setPodAntiAffinity(from.getPodAntiAffinity());
    } else if (from.getPodAntiAffinity() != null) {
      copyValues(to.getPodAntiAffinity(), from.getPodAntiAffinity());
    }
  }

  private void copyValues(V1NodeAffinity to, V1NodeAffinity from) {
    if (to.getPreferredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setPreferredDuringSchedulingIgnoredDuringExecution(from.getPreferredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getPreferredDuringSchedulingIgnoredDuringExecution() != null) {
      from.getPreferredDuringSchedulingIgnoredDuringExecution()
          .forEach(to::addPreferredDuringSchedulingIgnoredDuringExecutionItem);
    }
    if (to.getRequiredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setRequiredDuringSchedulingIgnoredDuringExecution(from.getRequiredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getRequiredDuringSchedulingIgnoredDuringExecution() != null) {
      copyValues(to.getRequiredDuringSchedulingIgnoredDuringExecution(),
          from.getRequiredDuringSchedulingIgnoredDuringExecution());
    }
  }

  private void copyValues(V1NodeSelector to,V1NodeSelector from) {
    if (to.getNodeSelectorTerms() == null) {
      to.setNodeSelectorTerms(from.getNodeSelectorTerms());
    } else if (from.getNodeSelectorTerms() != null) {
      from.getNodeSelectorTerms().forEach(to::addNodeSelectorTermsItem);
    }
  }

  private void copyValues(V1PodAffinity to, V1PodAffinity from) {
    if (to.getPreferredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setPreferredDuringSchedulingIgnoredDuringExecution(from.getPreferredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getPreferredDuringSchedulingIgnoredDuringExecution() != null) {
      from.getPreferredDuringSchedulingIgnoredDuringExecution()
          .forEach(to::addPreferredDuringSchedulingIgnoredDuringExecutionItem);
    }
    if (to.getRequiredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setRequiredDuringSchedulingIgnoredDuringExecution(from.getRequiredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getRequiredDuringSchedulingIgnoredDuringExecution() != null) {
      from.getRequiredDuringSchedulingIgnoredDuringExecution()
          .forEach(to::addRequiredDuringSchedulingIgnoredDuringExecutionItem);
    }
  }

  private void copyValues(V1PodAntiAffinity to, V1PodAntiAffinity from) {
    if (to.getPreferredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setPreferredDuringSchedulingIgnoredDuringExecution(from.getPreferredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getPreferredDuringSchedulingIgnoredDuringExecution() != null) {
      from.getPreferredDuringSchedulingIgnoredDuringExecution()
          .forEach(to::addPreferredDuringSchedulingIgnoredDuringExecutionItem);
    }
    if (to.getRequiredDuringSchedulingIgnoredDuringExecution() == null) {
      to.setRequiredDuringSchedulingIgnoredDuringExecution(from.getRequiredDuringSchedulingIgnoredDuringExecution());
    } else if (from.getRequiredDuringSchedulingIgnoredDuringExecution() != null) {
      from.getRequiredDuringSchedulingIgnoredDuringExecution()
          .forEach(to::addRequiredDuringSchedulingIgnoredDuringExecutionItem);
    }
  }

  Shutdown getShutdown() {
    return this.shutdown;
  }

  void setShutdown(String shutdownType, Long timeoutSeconds, Boolean ignoreSessions) {
    this.shutdown
        .shutdownType(shutdownType)
        .timeoutSeconds(timeoutSeconds)
        .ignoreSessions(ignoreSessions);
  }

  ProbeTuning getReadinessProbeTuning() {
    return this.readinessProbe;
  }

  void setReadinessProbeTuning(Integer initialDelay, Integer timeout, Integer period) {
    this.readinessProbe
        .initialDelaySeconds(initialDelay)
        .timeoutSeconds(timeout)
        .periodSeconds(period);
  }

  ProbeTuning getLivenessProbeTuning() {
    return this.livenessProbe;
  }

  void setLivenessProbe(Integer initialDelay, Integer timeout, Integer period) {
    this.livenessProbe
        .initialDelaySeconds(initialDelay)
        .timeoutSeconds(timeout)
        .periodSeconds(period);
  }

  void fillInFrom(ServerPod serverPod1) {
    for (V1EnvVar var : serverPod1.getV1EnvVars()) {
      addIfMissing(var);
    }
    livenessProbe.copyValues(serverPod1.livenessProbe);
    readinessProbe.copyValues(serverPod1.readinessProbe);
    shutdown.copyValues(serverPod1.shutdown);
    for (V1Volume var : serverPod1.getAdditionalVolumes()) {
      addIfMissing(var);
    }
    for (V1VolumeMount var : serverPod1.getAdditionalVolumeMounts()) {
      addIfMissing(var);
    }
    for (V1Container c : serverPod1.getInitContainers()) {
      addInitContainerIfMissing(c);
    }
    for (V1Container c : serverPod1.getContainers()) {
      addContainerIfMissing(c);
    }
    fillInFrom((KubernetesResource) serverPod1);
    serverPod1.nodeSelector.forEach(nodeSelector::putIfAbsent);
    copyValues(resources, serverPod1.resources);
    copyValues(podSecurityContext, serverPod1.podSecurityContext);
    copyValues(containerSecurityContext, serverPod1.containerSecurityContext);
    if (affinity == null) {
      affinity = serverPod1.affinity;
    } else if (serverPod1.affinity != null) {
      copyValues(affinity, serverPod1.affinity);
    }
    if (priorityClassName == null) {
      priorityClassName = serverPod1.priorityClassName;
    }
    readinessGates.addAll(serverPod1.readinessGates);
    if (restartPolicy == null) {
      restartPolicy = serverPod1.restartPolicy;
    }
    if (runtimeClassName == null) {
      runtimeClassName = serverPod1.runtimeClassName;
    }
    if (nodeName == null) {
      nodeName = serverPod1.nodeName;
    }
    if (schedulerName == null) {
      schedulerName = serverPod1.schedulerName;
    }
    tolerations.addAll(serverPod1.tolerations);
  }

  private void addIfMissing(V1Volume var) {
    if (!hasVolumeName(var.getName())) {
      addAdditionalVolume(var);
    }
  }

  private void addIfMissing(V1EnvVar var) {
    if (!hasEnvVar(var.getName())) {
      addEnvVar(var);
    }
  }

  private void addIfMissing(V1VolumeMount var) {
    if (!hasVolumeMountName(var.getName())) {
      addAdditionalVolumeMount(var);
    }
  }

  private void addInitContainerIfMissing(V1Container c) {
    if (!hasInitContainerName(c.getName())) {
      addInitContainer(c);
    }
  }

  private void addContainerIfMissing(V1Container c) {
    if (!hasContainerName(c.getName())) {
      addContainer(c);
    }
  }

  private List<V1EnvVar> getV1EnvVars() {
    return Optional.ofNullable(getEnv()).orElse(emptyList());
  }

  private boolean hasEnvVar(String name) {
    if (env == null) {
      return false;
    }
    for (V1EnvVar var : env) {
      if (var.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasVolumeName(String name) {
    for (V1Volume var : volumes) {
      if (var.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasVolumeMountName(String name) {
    for (V1VolumeMount var : volumeMounts) {
      if (var.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasInitContainerName(String name) {
    for (V1Container c : initContainers) {
      if (c.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasContainerName(String name) {
    for (V1Container c : containers) {
      if (c.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  List<V1EnvVar> getEnv() {
    return this.env;
  }

  void setEnv(@Nullable List<V1EnvVar> env) {
    this.env = env;
  }

  void addEnvVar(V1EnvVar var) {
    if (this.env == null) {
      setEnv(new ArrayList<>());
    }
    this.env.add(var);
  }

  Map<String, String> getNodeSelector() {
    return nodeSelector;
  }

  void addNodeSelector(String labelKey, String labelValue) {
    this.nodeSelector.put(labelKey, labelValue);
  }

  V1ResourceRequirements getResourceRequirements() {
    return resources;
  }

  void addRequestRequirement(String resource, String quantity) {
    resources.putRequestsItem(resource, Quantity.fromString(quantity));
  }

  void addLimitRequirement(String resource, String quantity) {
    resources.putLimitsItem(resource, Quantity.fromString(quantity));
  }

  V1PodSecurityContext getPodSecurityContext() {
    return podSecurityContext;
  }

  void setPodSecurityContext(V1PodSecurityContext podSecurityContext) {
    this.podSecurityContext = podSecurityContext;
  }

  List<V1Container> getInitContainers() {
    return initContainers;
  }

  void setInitContainers(List<V1Container> initContainers) {
    this.initContainers = initContainers;
  }

  void addInitContainer(V1Container initContainer) {
    initContainers.add(initContainer);
  }

  List<V1Container> getContainers() {
    return containers;
  }

  void setContainers(List<V1Container> containers) {
    this.containers = containers;
  }

  void addContainer(V1Container container) {
    containers.add(container);
  }

  V1SecurityContext getContainerSecurityContext() {
    return containerSecurityContext;
  }

  void setContainerSecurityContext(V1SecurityContext containerSecurityContext) {
    this.containerSecurityContext = containerSecurityContext;
  }

  void addAdditionalVolume(String name, String path) {
    addAdditionalVolume(
        new V1Volume().name(name).hostPath(new V1HostPathVolumeSource().path(path)));
  }

  private void addAdditionalVolume(V1Volume var) {
    volumes.add(var);
  }

  void addAdditionalVolumeMount(String name, String path) {
    addAdditionalVolumeMount(new V1VolumeMount().name(name).mountPath(path));
  }

  private void addAdditionalVolumeMount(V1VolumeMount var) {
    volumeMounts.add(var);
  }

  List<V1Volume> getAdditionalVolumes() {
    return volumes;
  }

  List<V1VolumeMount> getAdditionalVolumeMounts() {
    return volumeMounts;
  }

  V1Affinity getAffinity() {
    return affinity;
  }

  void setAffinity(V1Affinity affinity) {
    this.affinity = affinity;
  }

  String getPriorityClassName() {
    return priorityClassName;
  }

  void setPriorityClassName(String priorityClassName) {
    this.priorityClassName = priorityClassName;
  }

  List<V1PodReadinessGate> getReadinessGates() {
    return readinessGates;
  }

  void setReadinessGates(List<V1PodReadinessGate> readinessGates) {
    this.readinessGates = readinessGates;
  }

  void addReadinessGate(V1PodReadinessGate readinessGate) {
    readinessGates.add(readinessGate);
  }

  String getRestartPolicy() {
    return restartPolicy;
  }

  void setRestartPolicy(String restartPolicy) {
    this.restartPolicy = restartPolicy;
  }

  String getRuntimeClassName() {
    return runtimeClassName;
  }

  void setRuntimeClassName(String runtimeClassName) {
    this.runtimeClassName = runtimeClassName;
  }

  String getNodeName() {
    return nodeName;
  }

  void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  String getSchedulerName() {
    return schedulerName;
  }

  void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  List<V1Toleration> getTolerations() {
    return tolerations;
  }

  void setTolerations(List<V1Toleration> tolerations) {
    this.tolerations = tolerations;
  }

  void addToleration(V1Toleration toleration) {
    tolerations.add(toleration);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("env", env)
        .append("livenessProbe", livenessProbe)
        .append("readinessProbe", readinessProbe)
        .append("additionalVolumes", volumes)
        .append("additionalVolumeMounts", volumeMounts)
        .append("nodeSelector", nodeSelector)
        .append("resourceRequirements", resources)
        .append("podSecurityContext", podSecurityContext)
        .append("containerSecurityContext", containerSecurityContext)
        .append("initContainers", initContainers)
        .append("containers", containers)
        .append("shutdown", shutdown)
        .append("affinity", affinity)
        .append("priorityClassName", priorityClassName)
        .append("readinessGates", readinessGates)
        .append("restartPolicy", restartPolicy)
        .append("runtimeClassName", runtimeClassName)
        .append("nodeName", nodeName)
        .append("schedulerName", schedulerName)
        .append("tolerations", tolerations)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServerPod that = (ServerPod) o;

    return new EqualsBuilder()
        .appendSuper(super.equals(o))
        .append(
            Domain.sortOrNull(env, ENV_VAR_COMPARATOR),
            Domain.sortOrNull(that.env, ENV_VAR_COMPARATOR))
        .append(livenessProbe, that.livenessProbe)
        .append(readinessProbe, that.readinessProbe)
        .append(
            Domain.sortOrNull(volumes, VOLUME_COMPARATOR),
            Domain.sortOrNull(that.volumes, VOLUME_COMPARATOR))
        .append(
            Domain.sortOrNull(volumeMounts, VOLUME_MOUNT_COMPARATOR),
            Domain.sortOrNull(that.volumeMounts, VOLUME_MOUNT_COMPARATOR))
        .append(nodeSelector, that.nodeSelector)
        .append(resources, that.resources)
        .append(podSecurityContext, that.podSecurityContext)
        .append(containerSecurityContext, that.containerSecurityContext)
        .append(initContainers, that.initContainers)
        .append(containers, that.containers)
        .append(shutdown, that.shutdown)
        .append(affinity, that.affinity)
        .append(priorityClassName, that.priorityClassName)
        .append(readinessGates, that.readinessGates)
        .append(restartPolicy, that.restartPolicy)
        .append(runtimeClassName, that.runtimeClassName)
        .append(nodeName, that.nodeName)
        .append(schedulerName, that.schedulerName)
        .append(tolerations, that.tolerations)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .appendSuper(super.hashCode())
        .append(Domain.sortOrNull(env, ENV_VAR_COMPARATOR))
        .append(livenessProbe)
        .append(readinessProbe)
        .append(Domain.sortOrNull(volumes, VOLUME_COMPARATOR))
        .append(Domain.sortOrNull(volumeMounts, VOLUME_MOUNT_COMPARATOR))
        .append(nodeSelector)
        .append(resources)
        .append(podSecurityContext)
        .append(containerSecurityContext)
        .append(initContainers)
        .append(containers)
        .append(shutdown)
        .append(affinity)
        .append(priorityClassName)
        .append(readinessGates)
        .append(restartPolicy)
        .append(runtimeClassName)
        .append(nodeName)
        .append(schedulerName)
        .append(tolerations)
        .toHashCode();
  }
}
