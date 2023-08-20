// Generated with smithy-dart 0.3.1. DO NOT MODIFY.
// ignore_for_file: avoid_unused_constructor_parameters,deprecated_member_use_from_same_package,non_constant_identifier_names,require_trailing_commas

library smoke_test.ec2.model.delete_local_gateway_route_table_result; // ignore_for_file: no_leading_underscores_for_library_prefixes

import 'package:aws_common/aws_common.dart' as _i1;
import 'package:built_value/built_value.dart';
import 'package:built_value/serializer.dart';
import 'package:smithy/smithy.dart' as _i2;
import 'package:smoke_test/src/sdk/src/ec2/model/local_gateway_route_table.dart';

part 'delete_local_gateway_route_table_result.g.dart';

abstract class DeleteLocalGatewayRouteTableResult
    with
        _i1.AWSEquatable<DeleteLocalGatewayRouteTableResult>
    implements
        Built<DeleteLocalGatewayRouteTableResult,
            DeleteLocalGatewayRouteTableResultBuilder> {
  factory DeleteLocalGatewayRouteTableResult(
      {LocalGatewayRouteTable? localGatewayRouteTable}) {
    return _$DeleteLocalGatewayRouteTableResult._(
        localGatewayRouteTable: localGatewayRouteTable);
  }

  factory DeleteLocalGatewayRouteTableResult.build(
          [void Function(DeleteLocalGatewayRouteTableResultBuilder) updates]) =
      _$DeleteLocalGatewayRouteTableResult;

  const DeleteLocalGatewayRouteTableResult._();

  /// Constructs a [DeleteLocalGatewayRouteTableResult] from a [payload] and [response].
  factory DeleteLocalGatewayRouteTableResult.fromResponse(
    DeleteLocalGatewayRouteTableResult payload,
    _i1.AWSBaseHttpResponse response,
  ) =>
      payload;

  static const List<_i2.SmithySerializer<DeleteLocalGatewayRouteTableResult>>
      serializers = [DeleteLocalGatewayRouteTableResultEc2QuerySerializer()];

  /// Information about the local gateway route table.
  LocalGatewayRouteTable? get localGatewayRouteTable;
  @override
  List<Object?> get props => [localGatewayRouteTable];
  @override
  String toString() {
    final helper =
        newBuiltValueToStringHelper('DeleteLocalGatewayRouteTableResult')
          ..add(
            'localGatewayRouteTable',
            localGatewayRouteTable,
          );
    return helper.toString();
  }
}

class DeleteLocalGatewayRouteTableResultEc2QuerySerializer
    extends _i2.StructuredSmithySerializer<DeleteLocalGatewayRouteTableResult> {
  const DeleteLocalGatewayRouteTableResultEc2QuerySerializer()
      : super('DeleteLocalGatewayRouteTableResult');

  @override
  Iterable<Type> get types => const [
        DeleteLocalGatewayRouteTableResult,
        _$DeleteLocalGatewayRouteTableResult,
      ];
  @override
  Iterable<_i2.ShapeId> get supportedProtocols => const [
        _i2.ShapeId(
          namespace: 'aws.protocols',
          shape: 'ec2Query',
        )
      ];
  @override
  DeleteLocalGatewayRouteTableResult deserialize(
    Serializers serializers,
    Iterable<Object?> serialized, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result = DeleteLocalGatewayRouteTableResultBuilder();
    final iterator = serialized.iterator;
    while (iterator.moveNext()) {
      final key = iterator.current as String;
      iterator.moveNext();
      final value = iterator.current;
      if (value == null) {
        continue;
      }
      switch (key) {
        case 'localGatewayRouteTable':
          result.localGatewayRouteTable.replace((serializers.deserialize(
            value,
            specifiedType: const FullType(LocalGatewayRouteTable),
          ) as LocalGatewayRouteTable));
      }
    }

    return result.build();
  }

  @override
  Iterable<Object?> serialize(
    Serializers serializers,
    DeleteLocalGatewayRouteTableResult object, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result$ = <Object?>[
      const _i2.XmlElementName(
        'DeleteLocalGatewayRouteTableResultResponse',
        _i2.XmlNamespace('http://ec2.amazonaws.com/doc/2016-11-15'),
      )
    ];
    final DeleteLocalGatewayRouteTableResult(:localGatewayRouteTable) = object;
    if (localGatewayRouteTable != null) {
      result$
        ..add(const _i2.XmlElementName('LocalGatewayRouteTable'))
        ..add(serializers.serialize(
          localGatewayRouteTable,
          specifiedType: const FullType(LocalGatewayRouteTable),
        ));
    }
    return result$;
  }
}