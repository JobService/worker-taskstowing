--
-- Copyright 2021-2026 Open Text.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- This table is created here for integration testing purposes only.
-- The actual table is owned and created by the Job Service: https://github.com/JobService/job-service
-- Any changes made to the table in the Job Service should be made here as well.

DROP TABLE IF EXISTS public.stowed_task;

CREATE TABLE public.stowed_task (
  partition_id varchar(40) NOT NULL,
  job_id varchar(48) NOT NULL,
  task_classifier varchar(255) NOT NULL,
  task_api_version int4 NOT NULL,
  task_data bytea NOT NULL,
  task_status varchar(255) NOT NULL,
  "to" varchar(255) NOT NULL,
  tracking_info_job_task_id varchar(255) NOT NULL,
  tracking_info_last_status_check_time bigint NULL,
  tracking_info_status_check_interval_millis bigint NULL,
  tracking_info_status_check_url TEXT NULL,
  tracking_info_tracking_pipe varchar(255) NULL,
  tracking_info_track_to varchar(255) NULL,
  source_info bytea NULL,
  correlation_id varchar(255) NULL
--   CONSTRAINT fk_stowed_task FOREIGN KEY (partition_id, job_id) REFERENCES job(partition_id, job_id)
);
CREATE INDEX idx_partition_id_and_job_id_and_tracking_info_job_task_id ON public.stowed_task USING btree
(partition_id, job_id, tracking_info_job_task_id);
