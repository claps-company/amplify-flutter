// Generated with smithy-dart 0.3.1. DO NOT MODIFY.
// ignore_for_file: avoid_unused_constructor_parameters,deprecated_member_use_from_same_package,non_constant_identifier_names,require_trailing_commas

library smoke_test.ec2.model.describe_managed_prefix_lists_request; // ignore_for_file: no_leading_underscores_for_library_prefixes

import 'package:aws_common/aws_common.dart' as _i2;
import 'package:built_collection/built_collection.dart' as _i3;
import 'package:built_value/built_value.dart';
import 'package:built_value/serializer.dart';
import 'package:smithy/smithy.dart' as _i1;
import 'package:smoke_test/src/sdk/src/ec2/model/filter.dart';

part 'describe_managed_prefix_lists_request.g.dart';

abstract class DescribeManagedPrefixListsRequest
    with
        _i1.HttpInput<DescribeManagedPrefixListsRequest>,
        _i2.AWSEquatable<DescribeManagedPrefixListsRequest>
    implements
        Built<DescribeManagedPrefixListsRequest,
            DescribeManagedPrefixListsRequestBuilder> {
  factory DescribeManagedPrefixListsRequest({
    bool? dryRun,
    List<Filter>? filters,
    int? maxResults,
    String? nextToken,
    List<String>? prefixListIds,
  }) {
    dryRun ??= false;
    maxResults ??= 0;
    return _$DescribeManagedPrefixListsRequest._(
      dryRun: dryRun,
      filters: filters == null ? null : _i3.BuiltList(filters),
      maxResults: maxResults,
      nextToken: nextToken,
      prefixListIds:
          prefixListIds == null ? null : _i3.BuiltList(prefixListIds),
    );
  }

  factory DescribeManagedPrefixListsRequest.build(
          [void Function(DescribeManagedPrefixListsRequestBuilder) updates]) =
      _$DescribeManagedPrefixListsRequest;

  const DescribeManagedPrefixListsRequest._();

  factory DescribeManagedPrefixListsRequest.fromRequest(
    DescribeManagedPrefixListsRequest payload,
    _i2.AWSBaseHttpRequest request, {
    Map<String, String> labels = const {},
  }) =>
      payload;

  static const List<_i1.SmithySerializer<DescribeManagedPrefixListsRequest>>
      serializers = [DescribeManagedPrefixListsRequestEc2QuerySerializer()];

  @BuiltValueHook(initializeBuilder: true)
  static void _init(DescribeManagedPrefixListsRequestBuilder b) {
    b
      ..dryRun = false
      ..maxResults = 0;
  }

  /// Checks whether you have the required permissions for the action, without actually making the request, and provides an error response. If you have the required permissions, the error response is `DryRunOperation`. Otherwise, it is `UnauthorizedOperation`.
  bool get dryRun;

  /// One or more filters.
  ///
  /// *   `owner-id` \- The ID of the prefix list owner.
  ///
  /// *   `prefix-list-id` \- The ID of the prefix list.
  ///
  /// *   `prefix-list-name` \- The name of the prefix list.
  _i3.BuiltList<Filter>? get filters;

  /// The maximum number of results to return with a single call. To retrieve the remaining results, make another call with the returned `nextToken` value.
  int get maxResults;

  /// The token for the next page of results.
  String? get nextToken;

  /// One or more prefix list IDs.
  _i3.BuiltList<String>? get prefixListIds;
  @override
  DescribeManagedPrefixListsRequest getPayload() => this;
  @override
  List<Object?> get props => [
        dryRun,
        filters,
        maxResults,
        nextToken,
        prefixListIds,
      ];
  @override
  String toString() {
    final helper =
        newBuiltValueToStringHelper('DescribeManagedPrefixListsRequest')
          ..add(
            'dryRun',
            dryRun,
          )
          ..add(
            'filters',
            filters,
          )
          ..add(
            'maxResults',
            maxResults,
          )
          ..add(
            'nextToken',
            nextToken,
          )
          ..add(
            'prefixListIds',
            prefixListIds,
          );
    return helper.toString();
  }
}

class DescribeManagedPrefixListsRequestEc2QuerySerializer
    extends _i1.StructuredSmithySerializer<DescribeManagedPrefixListsRequest> {
  const DescribeManagedPrefixListsRequestEc2QuerySerializer()
      : super('DescribeManagedPrefixListsRequest');

  @override
  Iterable<Type> get types => const [
        DescribeManagedPrefixListsRequest,
        _$DescribeManagedPrefixListsRequest,
      ];
  @override
  Iterable<_i1.ShapeId> get supportedProtocols => const [
        _i1.ShapeId(
          namespace: 'aws.protocols',
          shape: 'ec2Query',
        )
      ];
  @override
  DescribeManagedPrefixListsRequest deserialize(
    Serializers serializers,
    Iterable<Object?> serialized, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result = DescribeManagedPrefixListsRequestBuilder();
    final iterator = serialized.iterator;
    while (iterator.moveNext()) {
      final key = iterator.current as String;
      iterator.moveNext();
      final value = iterator.current;
      if (value == null) {
        continue;
      }
      switch (key) {
        case 'DryRun':
          result.dryRun = (serializers.deserialize(
            value,
            specifiedType: const FullType(bool),
          ) as bool);
        case 'Filter':
          result.filters.replace((const _i1.XmlBuiltListSerializer(
            memberName: 'Filter',
            indexer: _i1.XmlIndexer.ec2QueryList,
          ).deserialize(
            serializers,
            value is String ? const [] : (value as Iterable<Object?>),
            specifiedType: const FullType(
              _i3.BuiltList,
              [FullType(Filter)],
            ),
          ) as _i3.BuiltList<Filter>));
        case 'MaxResults':
          result.maxResults = (serializers.deserialize(
            value,
            specifiedType: const FullType(int),
          ) as int);
        case 'NextToken':
          result.nextToken = (serializers.deserialize(
            value,
            specifiedType: const FullType(String),
          ) as String);
        case 'PrefixListId':
          result.prefixListIds.replace((const _i1.XmlBuiltListSerializer(
            memberName: 'item',
            indexer: _i1.XmlIndexer.ec2QueryList,
          ).deserialize(
            serializers,
            value is String ? const [] : (value as Iterable<Object?>),
            specifiedType: const FullType(
              _i3.BuiltList,
              [FullType(String)],
            ),
          ) as _i3.BuiltList<String>));
      }
    }

    return result.build();
  }

  @override
  Iterable<Object?> serialize(
    Serializers serializers,
    DescribeManagedPrefixListsRequest object, {
    FullType specifiedType = FullType.unspecified,
  }) {
    final result$ = <Object?>[
      const _i1.XmlElementName(
        'DescribeManagedPrefixListsRequestResponse',
        _i1.XmlNamespace('http://ec2.amazonaws.com/doc/2016-11-15'),
      )
    ];
    final DescribeManagedPrefixListsRequest(
      :dryRun,
      :filters,
      :maxResults,
      :nextToken,
      :prefixListIds
    ) = object;
    result$
      ..add(const _i1.XmlElementName('DryRun'))
      ..add(serializers.serialize(
        dryRun,
        specifiedType: const FullType(bool),
      ));
    if (filters != null) {
      result$
        ..add(const _i1.XmlElementName('Filter'))
        ..add(const _i1.XmlBuiltListSerializer(
          memberName: 'Filter',
          indexer: _i1.XmlIndexer.ec2QueryList,
        ).serialize(
          serializers,
          filters,
          specifiedType: const FullType.nullable(
            _i3.BuiltList,
            [FullType(Filter)],
          ),
        ));
    }
    result$
      ..add(const _i1.XmlElementName('MaxResults'))
      ..add(serializers.serialize(
        maxResults,
        specifiedType: const FullType(int),
      ));
    if (nextToken != null) {
      result$
        ..add(const _i1.XmlElementName('NextToken'))
        ..add(serializers.serialize(
          nextToken,
          specifiedType: const FullType(String),
        ));
    }
    if (prefixListIds != null) {
      result$
        ..add(const _i1.XmlElementName('PrefixListId'))
        ..add(const _i1.XmlBuiltListSerializer(
          memberName: 'item',
          indexer: _i1.XmlIndexer.ec2QueryList,
        ).serialize(
          serializers,
          prefixListIds,
          specifiedType: const FullType.nullable(
            _i3.BuiltList,
            [FullType(String)],
          ),
        ));
    }
    return result$;
  }
}