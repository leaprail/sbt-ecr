package sbtecr

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest
import sbt.Logger

private[sbtecr] object AwsSts extends Aws {

  def accountId(region: Region)(implicit logger: Logger): String = {
    val request = GetCallerIdentityRequest.builder().build()
    val response = sts(region).getCallerIdentity(request)

    logger.info(s"AWS account id: ${response.account()}")

    response.account()
  }

  private def sts(region: Region) = {
    StsClient.builder()
      .region(region)
      .credentialsProvider(credentialsProvider())
      .build()
  }
}
