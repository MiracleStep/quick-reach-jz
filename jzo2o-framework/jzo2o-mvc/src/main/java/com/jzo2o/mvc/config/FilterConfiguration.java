package com.jzo2o.mvc.config;

import com.jzo2o.mvc.filter.PackResultFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author mirac 
 */
@Configuration
@Import(PackResultFilter.class)
public class FilterConfiguration {
}
