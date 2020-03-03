/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flink.sql.launcher.perjob;

import com.dtstack.flink.sql.launcher.utils.SubmitUtil;
import com.dtstack.flink.sql.option.Options;
import com.dtstack.flink.sql.util.PluginUtil;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClientProvider;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLDecoder;
import java.util.Properties;


/**
 * per job mode submitter
 * Date: 2018/11/17
 * Company: www.dtstack.com
 * @author xuchao
 */

public class PerJobSubmitter {

    private static final Logger LOG = LoggerFactory.getLogger(PerJobSubmitter.class);

    public static String submit(Options launcherOptions, JobGraph jobGraph, Configuration flinkConfig) throws Exception {
		if (!StringUtils.isBlank(launcherOptions.getAddjar())) {
            SubmitUtil.fillUserJarForJobGraph(launcherOptions.getAddjar(),jobGraph);
		}

		String confProp = launcherOptions.getConfProp();
        confProp = URLDecoder.decode(confProp, Charsets.UTF_8.toString());
        Properties confProperties = PluginUtil.jsonStrToObject(confProp, Properties.class);
        ClusterSpecification clusterSpecification = FLinkPerJobResourceUtil.createClusterSpecification(confProperties);

        PerJobClusterClientBuilder perJobClusterClientBuilder = new PerJobClusterClientBuilder();
        perJobClusterClientBuilder.init(launcherOptions.getYarnconf(), flinkConfig, confProperties);

        String flinkJarPath = launcherOptions.getFlinkJarPath();
        YarnClusterDescriptor yarnClusterDescriptor = perJobClusterClientBuilder.createPerJobClusterDescriptor(flinkJarPath, launcherOptions, jobGraph);
        ClusterClientProvider<ApplicationId> applicationIdClusterClientProvider = yarnClusterDescriptor.deployJobCluster(clusterSpecification, jobGraph, true);
        String applicationId = applicationIdClusterClientProvider.getClusterClient().getClusterId().toString();
        String flinkJobId = jobGraph.getJobID().toString();

        String tips = String.format("deploy per_job with appId: %s, jobId: %s", applicationId, flinkJobId);
        System.out.println(tips);
        LOG.info(tips);

        return applicationId;
    }



}
