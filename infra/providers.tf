terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = "5.22.0"
    }
  }
  backend "s3" {
    bucket = "pgr301-2021-terraform-state"
    key    = "bean016/apprunner-actions.state"
    region = "eu-north-1"
      
  }
}

provider "aws" {
  # Configuration options
}