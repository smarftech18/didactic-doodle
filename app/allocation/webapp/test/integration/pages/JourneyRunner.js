sap.ui.define([
    "sap/fe/test/JourneyRunner",
	"arh/allocation/test/integration/pages/AllocationHistoryList",
	"arh/allocation/test/integration/pages/AllocationHistoryObjectPage"
], function (JourneyRunner, AllocationHistoryList, AllocationHistoryObjectPage) {
    'use strict';

    var runner = new JourneyRunner({
        launchUrl: sap.ui.require.toUrl('arh/allocation') + '/test/flpSandbox.html#arhallocation-tile',
        pages: {
			onTheAllocationHistoryList: AllocationHistoryList,
			onTheAllocationHistoryObjectPage: AllocationHistoryObjectPage
        },
        async: true
    });

    return runner;
});

