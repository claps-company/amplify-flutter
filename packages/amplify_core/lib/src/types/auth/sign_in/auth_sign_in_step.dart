// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

/// {@category Auth}
/// {@template amplify_core.auth.auth_sign_in_step}
/// The current step in the sign in flow.
/// {@endtemplate}
enum AuthSignInStep {
  /// The sign-in is not complete and the user must select and set up
  /// an MFA method.
  continueSignInWithMfaSelection,

  /// The sign-in is not complete and the user must select an MFA method to setup.
  continueSignInWithMfaSetupSelection,

  /// The sign-in is not complete and a TOTP authenticator app must be
  /// registered before continuing.
  continueSignInWithTotpSetup,

  /// The sign-in is not complete and an Email MFA must be set up before
  /// continuing.
  continueSignInWithEmailMfaSetup,

  /// The sign-in is not complete and must be confirmed with an SMS code.
  confirmSignInWithSmsMfaCode,

  /// The sign-in is not complete and must be confirmed with a TOTP code
  /// from a registered authenticator app.
  confirmSignInWithTotpMfaCode,

  /// The sign-in is not complete and must be confirmed with an email code.
  confirmSignInWithOtpCode,

  /// The sign-in is not complete and must be confirmed with the user's new
  /// password.
  confirmSignInWithNewPassword,

  /// The sign-in is not complete and must be confirmed with the answer to a
  /// custom challenge.
  confirmSignInWithCustomChallenge,

  /// The sign-in is not complete and the user must reset their password before
  /// proceeding.
  resetPassword,

  /// The sign-in is not complete and the user's sign up must be confirmed
  /// before proceeding.
  confirmSignUp,

  /// The sign-in is complete.
  done,
}
