// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import 'package:amplify_authenticator/src/l10n/generated/message_localizations.dart';

/// The translations for English (`en`).
class AuthenticatorMessageLocalizationsEn
    extends AuthenticatorMessageLocalizations {
  AuthenticatorMessageLocalizationsEn([super.locale = 'en']);

  @override
  String codeSent(String destination) {
    return 'A confirmation code has been sent to $destination.';
  }

  @override
  String get codeSentUnknown => 'A confirmation code has been sent.';

  @override
  String get copySucceeded => 'Copied to clipboard!';

  @override
  String get copyFailed => 'Copy to clipboard failed.';

  @override
  String get authenticationFailed => 'Incorrect username or password.';

  @override
  String get sessionExpired => 'Session has expired. Please sign in again.';

  @override
  String get userAlreadyExists =>
      'An account with the given email already exists.';
}
