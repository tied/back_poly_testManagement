package com.thed.zephyr.je.service;

import com.google.common.base.Optional;
import com.thed.zephyr.je.vo.SprintBean;

/**
 * Created by smangal on 9/29/15.
 */
public interface ZephyrSprintService {

    Optional<SprintBean> getSprint(Long springId);
}
