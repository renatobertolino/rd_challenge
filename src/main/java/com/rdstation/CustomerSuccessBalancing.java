package com.rdstation;

// import org.springframework.util.Assert;
import com.rdstation.exceptions.AbstentionsException;
import com.rdstation.exceptions.CustomerSizeInvalidException;
import com.rdstation.exceptions.CustomerSuccessSizeInvalidException;
import com.rdstation.exceptions.InvalidIdCustomerException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomerSuccessBalancing {

  private final List<Customer> customers;
  private final List<CustomerSuccess> customerSuccessWithoutAways;
  private final int firstScoreCustomerSuccess;
  private final int FIRST_ELEMENT = 0;

  public CustomerSuccessBalancing(
      List<CustomerSuccess> customerSuccess,
      List<Customer> customers,
      List<Integer> customerSuccessAway) {

    this.customers = customers;
    this.customerSuccessWithoutAways =
        customerSuccess.stream()
            .filter(
                currentCustomerSuccess ->
                    !customerSuccessAway.contains(currentCustomerSuccess.getId())
                        && currentCustomerSuccess.getId() > 0
                        && currentCustomerSuccess.getId() < 1000)
            .sorted(Comparator.comparingInt(CustomerSuccess::getScore))
            .collect(Collectors.toList());

    checkSizeCustomerSuccess();
    checkSizeCustomer(customers);
    checkAbstentions(customers, customerSuccessAway);

    this.firstScoreCustomerSuccess = customerSuccessWithoutAways.get(FIRST_ELEMENT).getScore();
  }

  public int run() {
    List<Integer> historyBalancing = new ArrayList<>();

    for (Customer currentCustomer : this.customers) {

      checkIdCustomer(currentCustomer);

      AtomicInteger maxScoreFound = new AtomicInteger(this.firstScoreCustomerSuccess);

      boolean customerServed = findMaxScore(currentCustomer, maxScoreFound);

      if (customerServed) {
        historyBalancing.add(maxScoreFound.intValue());
      }
    }
    return checkAttendantWithMoreCustomers(historyBalancing);
  }

  private boolean findMaxScore(Customer currentCustomer, AtomicInteger maxScoreFound) {
    boolean scoreFound= false;
    //Change recursion
    for (CustomerSuccess currentCustomerSuccess : this.customerSuccessWithoutAways) {
      if (currentCustomer.getScore() <= currentCustomerSuccess.getScore()) {
        maxScoreFound.set(currentCustomerSuccess.getId());
        scoreFound = true;
        break;
      }
    }
    return scoreFound;
  }

  private void checkIdCustomer(Customer currentCustomer) {
    if (currentCustomer.getId() == 0 || currentCustomer.getId() > 1000000) {
      throw new InvalidIdCustomerException();
    }
  }

  public int checkAttendantWithMoreCustomers(List<Integer> list) {
    Map<Integer, Long> elementCountMap =
        list.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    List<Integer> result =
        elementCountMap.values().stream()
            .max(Long::compareTo)
            .map(
                maxValue ->
                    elementCountMap.entrySet().stream()
                        .filter(entry -> maxValue.equals(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()))
            .orElse(Collections.emptyList());

    if (result.size() == 1) {
      return result.get(FIRST_ELEMENT);
    }

    return 0;
  }

  private void checkAbstentions(List<Customer> customers, List<Integer> customerSuccessAway) {
    if (customerSuccessAway.size() > (customers.size() / 2)) {
      throw new AbstentionsException();
    }
  }

  private void checkSizeCustomer(List<Customer> customers) {
    if (customers.size() == 0 || customers.size() >= 1000000) {
      throw new CustomerSizeInvalidException();
    }
  }

  private void checkSizeCustomerSuccess() {
    if (customerSuccessWithoutAways.size() == 0 || customerSuccessWithoutAways.size() >= 1000) {
      throw new CustomerSuccessSizeInvalidException();
    }
  }
}
