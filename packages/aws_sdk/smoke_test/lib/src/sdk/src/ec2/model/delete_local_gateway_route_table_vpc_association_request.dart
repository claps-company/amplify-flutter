// Generated with smithy-dart 0.3.1. DO NOT MODIFY.
// ignore_for_file: avoid_unused_constructor_parameters,deprecated_member_use_from_same_package,non_constant_identifier_names,require_trailing_commas

library smoke_test.ec2.model.delete_local_gateway_route_table_vpc_association_request; // ignore_for_file: no_leading_underscores_for_library_prefixes

import 'package:aws_common/aws_common.dart' as _i2;
import 'package:built_value/built_value.dart';
import 'package:built_value/serializer.dart';
import 'package:smithy/smithy.dart' as _i1;

part 'delete_local_gateway_route_table_vpc_association_request.g.dart';

abstract class DeleteLocalGatewayRouteTableVpcAssociationRequest
    with
        _i1.HttpInput<DeleteLocalGatewayRouteTableVpcAssociationRequest>,
        _i2.AWSEquatable<DeleteLocalGatewayRouteTableVpcAssociationRequest>
    implements
        Built<DeleteLocalGatewayRouteTableVpcAssociationRequest,
            DeleteLocalGatewayRouteTableVpcAssociationRequestBuilder> {
  factory DeleteLocalGatewayRouteTableVpcAssociationRequest({
    String? localGatewayRouteTableVpcAssociationId,
    bool? dryRun,
  }) {
    dryRun ??= false;
    return _$DeleteLocalGatewayRouteTableVpcAssociationRequest._(
      localGatewayRouteTableVpcAssociationId:
          localGatewayRouteTableVpcAssociationId,
      dryRun: dryRun,
    );
  }

  factory DeleteLocalGatewayRouteTableVpcAssociationRequest.build(
      [void Function(DeleteLocalGatewayRouteTableVpcAssociationRequestBuilder)
          updates]) = _$DeleteLocalGatewayRouteTableVpcAssociationRequest;

  const DeleteLocalGatewayRouteTableVpcAssociationRequest._();

  factory DeleteLocalGatewayRouteTableVpcAssociationRequest.fromRequest(
    DeleteLocalGatewayRouteTableVpcAssociationRequest payload,
    _i2.AWSBaseHttpRequest request, {
    Map<String, String> labels = const {},
  }) =>
      payload;

  static const List<
          _i1
          .SmithySerializer<DeleteLocalGatewayRouteTableVpcAssociationRequest>>
      serializers = [
    DeleteLocalGatewayRouteTableVpcAssociationRequestEc2QuerySerializer()
  ];

  @BuiltValueHook(initializeBuilder: true)
  static void _init(
      DeleteLocalGatewayRouteTableVpcAssociationRequestBuilder b) {
    b.dryRun = false;
  }

  /// The ID of the association.
  String? get localGatewayRouteTableVpcAssociationId;

  /// Checks whether you have the required permissions for the action, without actually making the request, and provides an error response. If you have the required permissions, the error response is `DryRunOperation`. Otherwise, it is `UnauthorizedOperation`.
  bool get dryRun;
  @override
  DeleteLocalGatewayRouteTableVpcAssociationRequest getPayload() => this;
  @override
  List<Object?> get props => [
        localGatewayRouteTableVpcAssociationId,
        dryRun,
      ];
  @override
  String toString() {
    final helper = newBuiltValueToStringHelper(
        'DeleteLocalGatewayRouteTableVpcAssociationRequest')
      ..add(
        'localGatewayRouteTableVpcAssociationId',
        localGatewayRouteTableVpcAssociationId,
      )
      ..add(
        'dryRun',
        dryRun,
      );
    return helper.toString();
  }
}

class DeleteLocalGatewayRouteTableVpcAssociationRequestEc2QuerySerializer
    extends _i1.StructuredSmithySerializer<
        DeleteLocalGatewayRouteTableVpcAssociationRequest> {
  const DeleteLocalGatewayRouteTableVpcAssociationRequestEc2QuerySerializer()
      : super('DeleteLocalGatewayRouteTableVpcAssociationRequest');

  @override
  Iterable<Type> get types => const [
        DeleteLocalGatewayRouteTableVpcAssociationRequest,
        _$DeleteLocalGatewayRouteTableVpcAssociationRequest,
      ];
  @override
  Iterable<_i1.ShapeId> get supportedProtocols => const [
        _i1.ShapeId(
          namespace: 'aws.protocols',
          shape: 'ec2Query',
        )
      ];
  @override
  DeleteLocalGatewayRouteTableVpcAssociationRequest deserialize(
    Serializers serializers,
    Iterable<Object?> serialized, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result = DeleteLocalGatewayRouteTableVpcAssociationRequestBuilder();
    final iterator = serialized.iterator;
    while (iterator.moveNext()) {
      final key = iterator.current as String;
      iterator.moveNext();
      final value = iterator.current;
      if (value == null) {
        continue;
      }
      switch (key) {
        case 'LocalGatewayRouteTableVpcAssociationId':
          result.localGatewayRouteTableVpcAssociationId =
              (serializers.deserialize(
            value,
            specifiedType: const FullType(String),
          ) as String);
        case 'DryRun':
          result.dryRun = (serializers.deserialize(
            value,
            specifiedType: const FullType(bool),
          ) as bool);
      }
    }

    return result.build();
  }

  @override
  Iterable<Object?> serialize(
    Serializers serializers,
    DeleteLocalGatewayRouteTableVpcAssociationRequest object, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result$ = <Object?>[
      const _i1.XmlElementName(
        'DeleteLocalGatewayRouteTableVpcAssociationRequestResponse',
        _i1.XmlNamespace('http://ec2.amazonaws.com/doc/2016-11-15'),
      )
    ];
    final DeleteLocalGatewayRouteTableVpcAssociationRequest(
      :localGatewayRouteTableVpcAssociationId,
      :dryRun
    ) = object;
    if (localGatewayRouteTableVpcAssociationId != null) {
      result$
        ..add(
            const _i1.XmlElementName('LocalGatewayRouteTableVpcAssociationId'))
        ..add(serializers.serialize(
          localGatewayRouteTableVpcAssociationId,
          specifiedType: const FullType(String),
        ));
    }
    result$
      ..add(const _i1.XmlElementName('DryRun'))
      ..add(serializers.serialize(
        dryRun,
        specifiedType: const FullType(bool),
      ));
    return result$;
  }
}
