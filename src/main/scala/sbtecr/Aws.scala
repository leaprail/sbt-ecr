package sbtecr

import software.amazon.awssdk.auth.credentials._
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest
import software.amazon.awssdk.profiles.ProfileFile

private[sbtecr] trait Aws {
  
  /**
   * Get AWS credentials provider using AWS SDK v2
   */
  def credentialsProvider(): AwsCredentialsProvider = {
    val profileName = sys.env.getOrElse("AWS_PROFILE", sys.env.getOrElse("AWS_DEFAULT_PROFILE", "default"))
    
    // Create a credentials provider chain that tries multiple methods
    DefaultCredentialsProvider.builder()
      .profileName(profileName)
      .build()
  }
  
  /**
   * Create a region object from string
   */
  def toRegion(regionName: String): Region = {
    Region.of(regionName)
  }
}
