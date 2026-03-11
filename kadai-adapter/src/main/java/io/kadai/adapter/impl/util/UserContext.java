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

package io.kadai.adapter.impl.util;

import io.kadai.common.api.security.UserPrincipal;
import io.kadai.common.internal.util.CheckedSupplier;
import java.util.concurrent.CompletionException;
import javax.security.auth.Subject;

public class UserContext {

  private UserContext() {
    throw new IllegalStateException("Utility class");
  }

  // Unchecked cast necessary to rethrow the original exception type
  // as Subject::callAs wraps every exception in a CompletionException
  @SuppressWarnings("unchecked")
  public static <T, E extends Exception> T runAsUser(
      String runAsUserId, CheckedSupplier<T, E> supplier) throws E {
    Subject subject = new Subject();
    subject.getPrincipals().add(new UserPrincipal(runAsUserId));
    try {
      return Subject.callAs(subject, supplier::get);
    } catch (CompletionException e) {
      try {
        throw ((E) e.getCause());
      } catch (ClassCastException ex) {
        throw new RuntimeException("Unexpected exception type", ex);
      }
    }
  }
}
