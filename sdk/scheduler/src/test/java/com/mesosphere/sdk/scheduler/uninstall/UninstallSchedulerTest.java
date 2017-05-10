package com.mesosphere.sdk.scheduler.uninstall;

import com.mesosphere.sdk.curator.CuratorStateStore;
import com.mesosphere.sdk.offer.ResourceUtils;
import com.mesosphere.sdk.scheduler.plan.Element;
import com.mesosphere.sdk.scheduler.plan.Plan;
import com.mesosphere.sdk.scheduler.plan.Status;
import com.mesosphere.sdk.state.StateStore;
import com.mesosphere.sdk.state.StateStoreCache;
import com.mesosphere.sdk.testutils.TaskTestUtils;
import com.mesosphere.sdk.testutils.TestConstants;
import org.apache.curator.test.TestingServer;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UninstallSchedulerTest {

    private static final String RESERVED_RESOURCE_1_ID = "reserved-resource-id";
    private static final String RESERVED_RESOURCE_2_ID = "reserved-volume-id";
    private static final String RESERVED_RESOURCE_3_ID = "reserved-cpu-id";
    private static final Protos.Resource RESERVED_RESOURCE_1 = ResourceUtils.getExpectedRanges(
            "ports",
            Collections.singletonList(Protos.Value.Range.newBuilder().setBegin(123).setEnd(234).build()),
            RESERVED_RESOURCE_1_ID,
            TestConstants.ROLE,
            TestConstants.PRINCIPAL);
    private static final Protos.Resource RESERVED_RESOURCE_2 = ResourceUtils.getExpectedRootVolume(
            999.0,
            RESERVED_RESOURCE_2_ID,
            TestConstants.CONTAINER_PATH,
            TestConstants.ROLE,
            TestConstants.PRINCIPAL,
            RESERVED_RESOURCE_2_ID);
    private static final Protos.Resource RESERVED_RESOURCE_3 = ResourceUtils.getExpectedScalar(
            "cpus",
            1.0,
            RESERVED_RESOURCE_3_ID,
            TestConstants.ROLE,
            TestConstants.PRINCIPAL);
    private static final Protos.TaskInfo TASK_A = TaskTestUtils.getTaskInfo(Arrays.asList(RESERVED_RESOURCE_1,
            RESERVED_RESOURCE_2, RESERVED_RESOURCE_3));
    private static TestingServer testingServer;
    private UninstallScheduler uninstallScheduler;
    @Mock
    private SchedulerDriver mockSchedulerDriver;

    @BeforeClass
    public static void beforeAll() throws Exception {
        testingServer = new TestingServer();
    }

    //    TODO: move to some test Utils class
    private static List<Status> getStepStatuses(Plan plan) {
        return plan.getChildren().stream()
                .flatMap(phase -> phase.getChildren().stream())
                .map(Element::getStatus)
                .collect(Collectors.toList());
    }

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        StateStoreCache.resetInstanceForTests();

        StateStore stateStore = StateStoreCache.getInstance(new CuratorStateStore("testing-uninstall",
                testingServer.getConnectString()));

        stateStore.storeTasks(Collections.singletonList(TASK_A));
        uninstallScheduler = new UninstallScheduler(0, java.time.Duration.ofSeconds(1), stateStore);
        uninstallScheduler.registered(mockSchedulerDriver, TestConstants.FRAMEWORK_ID, TestConstants.MASTER_INFO);
    }

    @Test
    public void testConstruction() {
        Assert.assertNotNull(uninstallScheduler);
    }

    @Test
    public void testEmptyOffers() throws Exception {
        uninstallScheduler.resourceOffers(mockSchedulerDriver, Collections.emptyList());
        verify(mockSchedulerDriver, times(1)).reconcileTasks(any());
        verify(mockSchedulerDriver, times(0)).acceptOffers(any(), anyCollectionOf(Protos.Offer.Operation.class), any());
        verify(mockSchedulerDriver, times(0)).declineOffer(any(), any());
    }

    @Test
    public void testInitialPlan() throws Exception {
        Plan plan = uninstallScheduler.uninstallPlanManager.getPlan();
        List<Status> expected = Arrays.asList(Status.PENDING, Status.PENDING, Status.PENDING,
                Status.PENDING, Status.PENDING);
        Assert.assertEquals(expected, getStepStatuses(plan));
    }

}