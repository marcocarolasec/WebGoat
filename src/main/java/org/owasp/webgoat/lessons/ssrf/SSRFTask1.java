/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.ssrf;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({"ssrf.hint1", "ssrf.hint2"})
public class SSRFTask1 implements AssignmentEndpoint {

  @PostMapping("/SSRF/task1")
  @ResponseBody
  public AttackResult completed(@RequestParam String url) {
    return stealTheCheese(url);
  }

  protected AttackResult stealTheCheese(String url) {
    var html = "<img class=\"image\" alt=\"Silly Cat\" src=\"images/cat.jpg\">";
    return failed(this).feedback("ssrf.failure").output(html).build();
  }
}
