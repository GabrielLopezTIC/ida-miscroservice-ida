package com.gabriel.core.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder(toBuilder = true)
@AllArgsConstructor
@Setter
@Getter
public class DispersorResponse {

    private List<String> dispersos; 
}
