package com.petlis.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.Objects;
/** Application commands only, never signed provider wire bytes. Member order is irrelevant; values and array order remain significant. */
final class LegacyCanonicalRequest {
 private LegacyCanonicalRequest(){}
 static boolean same(String current,String stored)throws JsonProcessingException{
  var json=new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);return Objects.equals(json.readTree(current),json.readTree(stored));
 }
}
