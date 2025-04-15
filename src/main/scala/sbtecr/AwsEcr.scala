package sbtecr

import java.util.Base64
import java.util.{List => JList, Collection => JCollection}

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ecr.EcrClient
import software.amazon.awssdk.services.ecr.model._
import sbt.Logger

// Java converters
import scala.collection.JavaConverters._

private[sbtecr] object AwsEcr extends Aws {

  def domain(region: Region, accountId: String): String = 
    s"${accountId}.dkr.ecr.${region.id()}.amazonaws.com"

  def createRepository(region: Region,
                       repositoryName: String,
                       imageTagsMutable: Boolean,
                       repositoryPolicyText: Option[String],
                       repositoryLifecyclePolicyText: Option[String])(implicit logger: Logger): Unit = {
    val client = ecr(region)

    try {
      val imageTagMutability = if (imageTagsMutable) ImageTagMutability.MUTABLE else ImageTagMutability.IMMUTABLE
      
      val request = CreateRepositoryRequest.builder()
        .repositoryName(repositoryName)
        .imageTagMutability(imageTagMutability)
        .build()
        
      val result = client.createRepository(request)
      logger.info(s"Repository created in ${region}: arn=${result.repository().repositoryArn()}")
      
      repositoryPolicyText.foreach(setPolicy(client, repositoryName, _))
      repositoryLifecyclePolicyText.foreach(putLifecyclePolicy(client, repositoryName, _))

    } catch {
      case e: RepositoryAlreadyExistsException =>
        logger.info(s"Repository exists: ${region}/${repositoryName}")
    }
  }

  private def setPolicy(ecr: EcrClient, repositoryName: String, repositoryPolicyText: String)(implicit logger: Logger): Unit = {
    val request = SetRepositoryPolicyRequest.builder()
      .repositoryName(repositoryName)
      .policyText(repositoryPolicyText)
      .build()
      
    ecr.setRepositoryPolicy(request)
    logger.info("Configured policy for ECR repository.")
  }

  private def putLifecyclePolicy(ecr: EcrClient, repositoryName: String, lifecyclePolicyText: String)(implicit logger: Logger): Unit = {
    val request = PutLifecyclePolicyRequest.builder()
      .repositoryName(repositoryName)
      .lifecyclePolicyText(lifecyclePolicyText)
      .build()
      
    ecr.putLifecyclePolicy(request)
    logger.info("Configured lifecycle policy for ECR repository.")
  }

  def dockerCredentials(region: Region, registryIds: Seq[String] = Seq.empty)(implicit logger: Logger): (String, String) = {
    val requestBuilder = GetAuthorizationTokenRequest.builder()
    val request = requestBuilder.build()

    val response = ecr(region).getAuthorizationToken(request)

    response
      .authorizationData().asScala
      .map(_.authorizationToken())
      .map(Base64.getDecoder.decode(_))
      .map(new String(_, "UTF-8"))
      .map(_.split(":"))
      .headOption match {
      case Some(creds) if creds.length == 2 =>
        (creds(0), creds(1))
      case _ =>
        throw new IllegalStateException("Authorization token not found.")
    }
  }

  private def ecr(region: Region): EcrClient = {
    EcrClient.builder()
      .region(region)
      .credentialsProvider(credentialsProvider())
      .build()
  }
}
