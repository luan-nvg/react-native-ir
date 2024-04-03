package com.danielr18.irmanager;

import static android.content.Context.CONSUMER_IR_SERVICE;

import android.hardware.ConsumerIrManager;
import android.hardware.ConsumerIrManager.CarrierFrequencyRange;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import java.math.BigInteger;
import java.util.Arrays; // Importe a classe Arrays

public class RNIRManagerModule extends ReactContextBaseJavaModule {

  private static final String MODULE_NAME = "RNIRManagerModule";
  private final ConsumerIrManager manager;

  public RNIRManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    manager =
      (ConsumerIrManager) reactContext.getSystemService(CONSUMER_IR_SERVICE);
  }

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @ReactMethod
  public void hasIrEmitter(Promise promise) {
    promise.resolve(manager.hasIrEmitter());
  }

  @ReactMethod
  public void getCarrierFrequencies(Promise promise) {
    try {
      CarrierFrequencyRange[] carrierFrequencyRanges = manager.getCarrierFrequencies();
      WritableArray carrierFrequencies = Arguments.createArray();

      for (CarrierFrequencyRange carrierFrequencyRange : carrierFrequencyRanges) {
        WritableMap carrierFrequency = Arguments.createMap();
        carrierFrequency.putInt(
          "minFrequency",
          carrierFrequencyRange.getMinFrequency()
        );
        carrierFrequency.putInt(
          "maxFrequency",
          carrierFrequencyRange.getMaxFrequency()
        );
        carrierFrequencies.pushMap(carrierFrequency);
      }

      promise.resolve(carrierFrequencies);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void transmit(
    Integer carrierFrequency,
    ReadableArray burstsPattern,
    Promise promise
  ) {
    int[] pattern = new int[burstsPattern.size()];

    for (int i = 0; i < burstsPattern.size(); i++) {
      pattern[i] = burstsPattern.getInt(i);
    }

    try {
      manager.transmit(carrierFrequency, pattern);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  private static final int MAX_PATTERN_LENGTH = 1000; // Defina o valor apropriado

  @ReactMethod
  public void transmitProntoCode(String prontoHexCode, Promise promise) {
    try {
      String[] codeParts = prontoHexCode.split(" ");

      int prontoClockFrequency = Integer.parseInt(codeParts[1], 16);
      double exactCarrierFrequency =
        1000000.0 / (prontoClockFrequency * 0.241246);

      int carrierFrequency = (int) exactCarrierFrequency;
      int firstSequenceBurstPairs = Integer.parseInt(codeParts[2], 16);
      int secondSequenceBurstPairs = Integer.parseInt(codeParts[3], 16);

      // Seu código para calcular o padrão de IR...

      // Aqui você pode verificar o comprimento do padrão de IR e, se for muito longo, reduzi-lo ou dividir em partes menores.

      // Exemplo de redução do padrão de IR (apenas para fins ilustrativos, ajuste conforme necessário):
      int[] pattern = new int[(firstSequenceBurstPairs * 2) +
      (secondSequenceBurstPairs * 2)];

      int i = 0;
      int firstPairIndex = 4;
      int secondPairIndex = firstPairIndex + (firstSequenceBurstPairs * 2);

      for (int j = firstPairIndex; j < secondPairIndex; i++, j++) {
        pattern[i] =
          Integer.parseInt(codeParts[j], 16) * (1000000 / carrierFrequency);
      }

      for (
        int j = secondPairIndex;
        j < secondPairIndex + (secondSequenceBurstPairs * 2);
        i++, j++
      ) {
        pattern[i] =
          Integer.parseInt(codeParts[j], 16) * (1000000 / carrierFrequency);
      }

      int[] reducedPattern = Arrays.copyOfRange(
        pattern,
        0,
        Math.min(pattern.length, MAX_PATTERN_LENGTH)
      );

      manager.transmit(carrierFrequency, reducedPattern);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject(e);
    }
  }
}
