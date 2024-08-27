import { defineBackend } from "@aws-amplify/backend";
import { addAnalyticsExtensions } from "infra-common";
import { auth } from "./auth/resource";

/**
 * @see https://docs.amplify.aws/react/build-a-backend/ to add storage, functions, and more
 */

const backend = defineBackend({
  auth,
});

const { cfnIdentityPool } = backend.auth.resources.cfnResources;
cfnIdentityPool.allowUnauthenticatedIdentities = false;

const pinpointRole = backend.auth.resources.authenticatedUserIamRole;
const unauthPinpointRole = backend.auth.resources.unauthenticatedUserIamRole;

const resources = backend.auth.resources;
const { userPool, cfnResources } = resources;
const { stack } = userPool;

const customOutputs = addAnalyticsExtensions({
  name: "analytics-main",
  stack: stack,
  authenticatedRole: pinpointRole,
  unauthenticatedRole: unauthPinpointRole,
});

// patch the custom Pinpoint resource to the expected output configuration
backend.addOutput(customOutputs);