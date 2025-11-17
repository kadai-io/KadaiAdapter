/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.camunda.parselistener;

import io.kadai.adapter.camunda.tasklistener.KadaiTaskListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is responsible for adding the KadaiTaskListener to all user tasks. */
public class KadaiParseListener extends AbstractBpmnParseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiParseListener.class);

  private boolean gotActivated = false;

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {

    if (!gotActivated) {
      gotActivated = true;
      LOGGER.info("KadaiParseListener activated successfully");
    }

    ActivityBehavior behavior = activity.getActivityBehavior();
    if (behavior instanceof UserTaskActivityBehavior) {

      TaskDefinition userTask = ((UserTaskActivityBehavior) behavior).getTaskDefinition();

      userTask.addTaskListener(TaskListener.EVENTNAME_CREATE, KadaiTaskListener.getInstance());
      userTask.addTaskListener(TaskListener.EVENTNAME_COMPLETE, KadaiTaskListener.getInstance());
      userTask.addTaskListener(TaskListener.EVENTNAME_DELETE, KadaiTaskListener.getInstance());
    }
  }
}
