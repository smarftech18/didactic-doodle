using AllocationHistoryService as service from '../../srv/service';
annotate service.AllocationHistory with @(
    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'ID',
                Value : ID,
            },
            {
                $Type : 'UI.DataField',
                Label : 'executedAt',
                Value : executedAt,
            },
            {
                $Type : 'UI.DataField',
                Label : 'executedBy',
                Value : executedBy,
            },
            {
                $Type : 'UI.DataField',
                Label : 'allocationType',
                Value : allocationType,
            },
            {
                $Type : 'UI.DataField',
                Label : 'allocationDestCode',
                Value : allocationDestCode,
            },
            {
                $Type : 'UI.DataField',
                Label : 'material',
                Value : material,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup',
        },
    ],
);

